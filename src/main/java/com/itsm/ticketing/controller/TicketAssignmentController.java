package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.AssignTicketRequest;
import com.itsm.ticketing.dto.TicketAssignmentResponse;
import com.itsm.ticketing.dto.UnassignTicketRequest;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.TicketAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing ticket assignments.
 * Provides endpoints for assigning, unassigning, and reassigning
 * support engineers to tickets.
 *
 * Access Control:
 * - Assign/Unassign/Reassign: ADMIN only
 * - View assignments by ticket: ADMIN, SUPPORT
 * - View my assignments: SUPPORT, ADMIN
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketAssignmentController {

    private final TicketAssignmentService assignmentService;

    /**
     * Assign support engineers to a ticket.
     * Supports assigning multiple support users at once.
     *
     * @param ticketId    the ticket ID
     * @param request     contains list of support user IDs and optional notes
     * @param currentUser the authenticated admin user
     * @return list of created assignments
     */
    @PostMapping("/{ticketId}/assign")
    public ResponseEntity<List<TicketAssignmentResponse>> assignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("POST /api/v1/tickets/{}/assign - Assigning {} support user(s) by {}",
                ticketId, request.getSupportUserIds().size(), currentUser.getEmail());

        List<TicketAssignmentResponse> responses =
                assignmentService.assignTicket(ticketId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Unassign support engineers from a ticket.
     *
     * @param ticketId    the ticket ID
     * @param request     contains list of support user IDs to unassign
     * @param currentUser the authenticated admin user
     */
    @PostMapping("/{ticketId}/unassign")
    public ResponseEntity<Void> unassignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody UnassignTicketRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("POST /api/v1/tickets/{}/unassign - Unassigning {} support user(s) by {}",
                ticketId, request.getSupportUserIds().size(), currentUser.getEmail());

        assignmentService.unassignTicket(ticketId, request, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reassign a ticket: remove all current assignments and assign new support engineers.
     *
     * @param ticketId    the ticket ID
     * @param request     contains list of new support user IDs
     * @param currentUser the authenticated admin user
     * @return list of new assignments
     */
    @PostMapping("/{ticketId}/reassign")
    public ResponseEntity<List<TicketAssignmentResponse>> reassignTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody AssignTicketRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("POST /api/v1/tickets/{}/reassign - Reassigning to {} support user(s) by {}",
                ticketId, request.getSupportUserIds().size(), currentUser.getEmail());

        List<TicketAssignmentResponse> responses =
                assignmentService.reassignTicket(ticketId, request, currentUser);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all active assignments for a specific ticket.
     * Accessible by ADMIN and SUPPORT.
     *
     * @param ticketId the ticket ID
     * @return list of active assignments
     */
    @GetMapping("/{ticketId}/assignments")
    public ResponseEntity<List<TicketAssignmentResponse>> getTicketAssignments(
            @PathVariable Long ticketId) {
        log.info("GET /api/v1/tickets/{}/assignments - Fetching assignments", ticketId);

        List<TicketAssignmentResponse> assignments =
                assignmentService.getAssignmentsByTicket(ticketId);
        return ResponseEntity.ok(assignments);
    }

    /**
     * Get all tickets assigned to the current user (my assignments).
     * Accessible by SUPPORT and ADMIN.
     *
     * @param currentUser the authenticated user
     * @return list of active assignments for this user
     */
    @GetMapping("/my-assignments")
    public ResponseEntity<List<TicketAssignmentResponse>> getMyAssignments(
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/tickets/my-assignments - Fetching assignments for {}",
                currentUser.getEmail());

        List<TicketAssignmentResponse> assignments =
                assignmentService.getMyAssignments(currentUser);
        return ResponseEntity.ok(assignments);
    }
}
