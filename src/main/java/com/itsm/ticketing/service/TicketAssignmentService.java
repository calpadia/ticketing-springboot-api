package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AssignTicketRequest;
import com.itsm.ticketing.dto.TicketAssignmentResponse;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.dto.UnassignTicketRequest;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.event.TicketEvent;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.TicketAssignmentRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing ticket assignments.
 * Handles assigning, unassigning, and reassigning support engineers to tickets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAssignmentService {

    private final TicketAssignmentRepository assignmentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Assign one or more support engineers to a ticket.
     * Only ADMIN can assign tickets.
     *
     * @param ticketId    the ticket ID
     * @param request     the assignment request with support user IDs
     * @param assignedBy  the admin user making the assignment
     * @return list of created assignments
     */
    @Transactional
    public List<TicketAssignmentResponse> assignTicket(Long ticketId, AssignTicketRequest request, User assignedBy) {
        // ADMIN bisa assign ke siapa saja (SUPPORT atau TECHNICAL_SUPPORT).
        // SUPPORT bisa eskalasi: assign ticket ke TECHNICAL_SUPPORT.
        if (assignedBy.getRole() != Role.ADMIN && assignedBy.getRole() != Role.SUPPORT) {
            throw new AccessDeniedException(
                    "Hanya ADMIN atau SUPPORT yang dapat melakukan assignment ticket");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        // Cannot assign closed tickets
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Tidak bisa assign ticket yang sudah CLOSED");
        }

        List<TicketAssignmentResponse> responses = new ArrayList<>();

        for (Long supportUserId : request.getSupportUserIds()) {
            User supportUser = userRepository.findById(supportUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with ID: " + supportUserId));

            // Validate target role: SUPPORT atau TECHNICAL_SUPPORT
            Role targetRole = supportUser.getRole();
            if (targetRole != Role.SUPPORT && targetRole != Role.TECHNICAL_SUPPORT) {
                throw new IllegalArgumentException(
                        "User " + supportUser.getName() + " (ID: " + supportUserId
                                + ") bukan SUPPORT/TECHNICAL_SUPPORT. Hanya kedua role tersebut yang bisa di-assign.");
            }

            // SUPPORT hanya boleh assign ke TECHNICAL_SUPPORT (eskalasi), tidak ke sesama SUPPORT
            if (assignedBy.getRole() == Role.SUPPORT && targetRole != Role.TECHNICAL_SUPPORT) {
                throw new AccessDeniedException(
                        "SUPPORT hanya bisa eskalasi ticket ke TECHNICAL_SUPPORT");
            }

            // Check if already assigned
            if (assignmentRepository.existsByTicketIdAndAssignedToIdAndActiveTrue(ticketId, supportUserId)) {
                log.warn("User {} already assigned to ticket {}, skipping",
                        supportUser.getEmail(), ticket.getTicketNumber());
                continue; // Skip duplicate assignment
            }

            TicketAssignment assignment = TicketAssignment.builder()
                    .ticket(ticket)
                    .assignedTo(supportUser)
                    .assignedBy(assignedBy)
                    .notes(request.getNotes())
                    .active(true)
                    .build();

            TicketAssignment saved = assignmentRepository.save(assignment);
            responses.add(mapToResponse(saved));

            log.info("Ticket {} assigned to {} by {}",
                    ticket.getTicketNumber(), supportUser.getName(), assignedBy.getName());
        }

        publishAssignedEvent(ticket);
        return responses;
    }

    private void publishAssignedEvent(Ticket ticket) {
        // Build a minimal TicketResponse that carries just enough info for frontend routing
        TicketResponse payload = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .clientId(ticket.getClient() != null ? ticket.getClient().getId() : null)
                .clientCompanyName(ticket.getClient() != null ? ticket.getClient().getCompanyName() : null)
                .build();
        eventPublisher.publishEvent(new TicketEvent(this, TicketEvent.Type.ASSIGNED, payload));
    }

    /**
     * Unassign one or more support engineers from a ticket.
     * Only ADMIN can unassign.
     *
     * @param ticketId   the ticket ID
     * @param request    the unassignment request
     * @param currentUser the admin user performing the action
     */
    @Transactional
    public void unassignTicket(Long ticketId, UnassignTicketRequest request, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPPORT) {
            throw new AccessDeniedException(
                    "Hanya ADMIN atau SUPPORT yang dapat melakukan unassignment ticket");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        for (Long supportUserId : request.getSupportUserIds()) {
            TicketAssignment assignment = assignmentRepository
                    .findByTicketIdAndAssignedToIdAndActiveTrue(ticketId, supportUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Assignment not found for user ID: " + supportUserId
                                    + " on ticket: " + ticket.getTicketNumber()));

            assignment.setActive(false);
            assignment.setUnassignedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);

            log.info("Ticket {} unassigned from user ID {} by {}. Reason: {}",
                    ticket.getTicketNumber(), supportUserId, currentUser.getName(),
                    request.getReason());
        }
        publishAssignedEvent(ticket);
    }

    /**
     * Reassign a ticket: unassign old support and assign new ones.
     * Only ADMIN can reassign.
     *
     * @param ticketId   the ticket ID
     * @param request    the new assignment request
     * @param currentUser the admin user performing the action
     * @return list of new assignments
     */
    @Transactional
    public List<TicketAssignmentResponse> reassignTicket(Long ticketId, AssignTicketRequest request, User currentUser) {
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.SUPPORT) {
            throw new AccessDeniedException(
                    "Hanya ADMIN atau SUPPORT yang dapat melakukan reassignment ticket");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        // Deactivate all current assignments
        List<TicketAssignment> currentAssignments =
                assignmentRepository.findByTicketIdAndActiveTrue(ticketId);

        for (TicketAssignment assignment : currentAssignments) {
            assignment.setActive(false);
            assignment.setUnassignedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);
        }

        log.info("Ticket {} reassigned: {} previous assignments deactivated",
                ticket.getTicketNumber(), currentAssignments.size());

        // Create new assignments
        return assignTicket(ticketId, request, currentUser);
    }

    /**
     * Get all active assignments for a ticket.
     *
     * @param ticketId the ticket ID
     * @return list of active assignments
     */
    @Transactional(readOnly = true)
    public List<TicketAssignmentResponse> getAssignmentsByTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with ID: " + ticketId);
        }

        return assignmentRepository.findByTicketIdAndActiveTrue(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all tickets assigned to the current support user.
     *
     * @param currentUser the authenticated support user
     * @return list of assignments for this user
     */
    @Transactional(readOnly = true)
    public List<TicketAssignmentResponse> getMyAssignments(User currentUser) {
        if (currentUser.getRole() != Role.SUPPORT
                && currentUser.getRole() != Role.TECHNICAL_SUPPORT
                && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Hanya SUPPORT, TECHNICAL_SUPPORT, dan ADMIN yang dapat melihat assignment");
        }

        return assignmentRepository.findByAssignedToIdAndActiveTrue(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private TicketAssignmentResponse mapToResponse(TicketAssignment assignment) {
        return TicketAssignmentResponse.builder()
                .id(assignment.getId())
                .ticketId(assignment.getTicket().getId())
                .ticketNumber(assignment.getTicket().getTicketNumber())
                .ticketTitle(assignment.getTicket().getTitle())
                .assignedToId(assignment.getAssignedTo().getId())
                .assignedToName(assignment.getAssignedTo().getName())
                .assignedToEmail(assignment.getAssignedTo().getEmail())
                .assignedById(assignment.getAssignedBy().getId())
                .assignedByName(assignment.getAssignedBy().getName())
                .notes(assignment.getNotes())
                .assignedAt(assignment.getAssignedAt())
                .active(assignment.isActive())
                .build();
    }
}
