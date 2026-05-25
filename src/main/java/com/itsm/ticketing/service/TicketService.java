package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AttachmentResponse;
import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketProgressLogResponse;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.dto.UpdateTicketStatusRequest;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.exception.QuotaExceededException;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientQuotaRepository;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.TicketProgressLogRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing tickets.
 * Handles business logic including quota validation and ticket number generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final ClientQuotaRepository clientQuotaRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final TicketProgressLogRepository progressLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Create a new ticket with optional file attachments.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Validates that the client and requester exist</li>
     *   <li>Checks the client's maintenance quota for the current year</li>
     *   <li>Increments the used quota counter atomically within the transaction</li>
     *   <li>Generates a unique ticket number in format TKT-YYYYMMDD-XXX</li>
     *   <li>Stores attached files to local filesystem (optional)</li>
     * </ul>
     *
     * @param request the ticket creation request
     * @param files   optional list of file attachments
     * @return the created ticket response
     * @throws ResourceNotFoundException if client or requester is not found
     * @throws QuotaExceededException    if the client's quota is exhausted
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, List<MultipartFile> files) {
        log.info("Creating ticket for client ID: {}, maintenance type: {}",
                request.getClientId(), request.getMaintenanceType());

        // 1. Validate client exists
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + request.getClientId()));

        // 2. Validate requester exists
        User requester = userRepository.findById(request.getRequesterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + request.getRequesterId()));

        // 3. Validate and update quota
        int currentYear = LocalDate.now().getYear();
        ClientQuota quota = clientQuotaRepository.findByClientIdAndYear(
                        request.getClientId(), currentYear)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quota not found for client ID: " + request.getClientId()
                                + " and year: " + currentYear));

        validateAndIncrementQuota(quota, request.getMaintenanceType());

        // 4. Generate unique ticket number
        String ticketNumber = generateTicketNumber();

        // 5. Build and save the ticket
        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .priority(request.getPriority())
                .maintenanceType(request.getMaintenanceType())
                .client(client)
                .requester(requester)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        clientQuotaRepository.save(quota);

        // 6. Store attachments if provided
        List<AttachmentResponse> attachmentResponses = Collections.emptyList();
        if (files != null && !files.isEmpty()) {
            attachmentResponses = fileStorageService.storeFiles(savedTicket, files);
            log.info("{} file(s) attached to ticket {}", attachmentResponses.size(), ticketNumber);
        }

        log.info("Ticket created successfully: {}", ticketNumber);

        return mapToResponse(savedTicket, attachmentResponses);
    }

    /**
     * Get all tickets visible to the current user.
     * ADMIN: sees all tickets. USER: sees only their client's tickets.
     *
     * @param currentUser the authenticated user
     * @return list of ticket responses
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(User currentUser) {
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ADMIN) {
            tickets = ticketRepository.findAll();
        } else {
            // USER: only see tickets from their own client
            if (currentUser.getClient() == null) {
                return Collections.emptyList();
            }
            tickets = ticketRepository.findByClientId(currentUser.getClient().getId());
        }

        return tickets.stream()
                .map(ticket -> {
                    List<AttachmentResponse> attachments =
                            fileStorageService.getAttachmentsByTicketId(ticket.getId());
                    return mapToResponse(ticket, attachments);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a ticket by its ID with access control.
     * ADMIN: can see any ticket. USER: can only see their client's tickets.
     *
     * @param id          the ticket ID
     * @param currentUser the authenticated user
     * @return the ticket response
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, User currentUser) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + id));
        validateTicketAccess(ticket, currentUser);
        List<AttachmentResponse> attachments =
                fileStorageService.getAttachmentsByTicketId(id);
        return mapToResponse(ticket, attachments);
    }

    /**
     * Get a ticket by its ticket number with access control.
     *
     * @param ticketNumber the unique ticket number
     * @param currentUser  the authenticated user
     * @return the ticket response
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketByNumber(String ticketNumber, User currentUser) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));
        validateTicketAccess(ticket, currentUser);
        List<AttachmentResponse> attachments =
                fileStorageService.getAttachmentsByTicketId(ticket.getId());
        return mapToResponse(ticket, attachments);
    }

    /**
     * Validates that the current user has access to this ticket.
     * ADMIN: always allowed. USER: only if the ticket belongs to their client.
     */
    private void validateTicketAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admin can access all tickets
        }
        if (currentUser.getClient() == null ||
                !currentUser.getClient().getId().equals(ticket.getClient().getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Anda tidak memiliki akses ke ticket ini");
        }
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Validates the quota and increments the used counter.
     * Throws QuotaExceededException if the quota is exhausted.
     */
    private void validateAndIncrementQuota(ClientQuota quota, MaintenanceType type) {
        switch (type) {
            case CM -> {
                if (quota.getCmUsed() >= quota.getCmQuota()) {
                    throw new QuotaExceededException(
                            "Kuota CM tidak mencukupi untuk client: "
                                    + quota.getClient().getCompanyName()
                                    + " (Used: " + quota.getCmUsed()
                                    + "/" + quota.getCmQuota() + ")");
                }
                quota.setCmUsed(quota.getCmUsed() + 1);
                log.info("CM quota updated: {}/{}", quota.getCmUsed(), quota.getCmQuota());
            }
            case PM -> {
                if (quota.getPmUsed() >= quota.getPmQuota()) {
                    throw new QuotaExceededException(
                            "Kuota PM tidak mencukupi untuk client: "
                                    + quota.getClient().getCompanyName()
                                    + " (Used: " + quota.getPmUsed()
                                    + "/" + quota.getPmQuota() + ")");
                }
                quota.setPmUsed(quota.getPmUsed() + 1);
                log.info("PM quota updated: {}/{}", quota.getPmUsed(), quota.getPmQuota());
            }
        }
    }

    /**
     * Generates a unique ticket number in format: TKT-YYYYMMDD-XXX
     * Example: TKT-20260522-001
     */
    private String generateTicketNumber() {
        String datePrefix = "TKT-" + LocalDate.now().format(DATE_FORMATTER) + "-";
        long count = ticketRepository.countByTicketNumberStartingWith(datePrefix);
        return datePrefix + String.format("%03d", count + 1);
    }

    /**
     * Maps a Ticket entity to a TicketResponse DTO.
     */
    private TicketResponse mapToResponse(Ticket ticket, List<AttachmentResponse> attachments) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .maintenanceType(ticket.getMaintenanceType())
                .clientId(ticket.getClient().getId())
                .clientCompanyName(ticket.getClient().getCompanyName())
                .requesterId(ticket.getRequester().getId())
                .requesterName(ticket.getRequester().getName())
                .attachments(attachments)
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    // ========================================================================
    // TICKET STATUS UPDATE
    // ========================================================================

    /**
     * Update the status of a ticket and record the change in progress log.
     *
     * @param ticketId the ticket ID
     * @param request  the status update request
     * @return the updated ticket response
     */
    @Transactional
    public TicketResponse updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request) {
        log.info("Updating ticket {} status to {}", ticketId, request.getStatus());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        User changedBy = userRepository.findById(request.getChangedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + request.getChangedBy()));

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // Update ticket status
        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        // Record progress log
        TicketProgressLog progressLog = TicketProgressLog.builder()
                .ticket(ticket)
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .changedBy(changedBy)
                .notes(request.getNotes())
                .build();
        progressLogRepository.save(progressLog);

        log.info("Ticket {} status updated: {} -> {} by user {}",
                ticket.getTicketNumber(), oldStatus, newStatus, changedBy.getName());

        List<AttachmentResponse> attachments =
                fileStorageService.getAttachmentsByTicketId(ticketId);
        return mapToResponse(updatedTicket, attachments);
    }

    /**
     * Get the progress history of a ticket.
     *
     * @param ticketId the ticket ID
     * @return list of progress log entries
     */
    @Transactional(readOnly = true)
    public List<TicketProgressLogResponse> getProgressLogs(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with ID: " + ticketId);
        }
        return progressLogRepository.findByTicketIdOrderByChangedAtAsc(ticketId)
                .stream()
                .map(this::mapToProgressLogResponse)
                .collect(Collectors.toList());
    }

    /**
     * Validates allowed status transitions.
     * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
     * Also allows: RESOLVED -> IN_PROGRESS (reopen)
     */
    private void validateStatusTransition(TicketStatus from, TicketStatus to) {
        if (from == to) {
            throw new IllegalArgumentException(
                    "Ticket sudah berstatus: " + from);
        }
        boolean valid = switch (from) {
            case OPEN -> to == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TicketStatus.RESOLVED || to == TicketStatus.CLOSED;
            case RESOLVED -> to == TicketStatus.CLOSED || to == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Transisi status tidak valid: " + from + " -> " + to);
        }
    }

    /**
     * Maps a TicketProgressLog entity to response DTO.
     */
    private TicketProgressLogResponse mapToProgressLogResponse(TicketProgressLog logEntry) {
        return TicketProgressLogResponse.builder()
                .id(logEntry.getId())
                .ticketId(logEntry.getTicket().getId())
                .ticketNumber(logEntry.getTicket().getTicketNumber())
                .fromStatus(logEntry.getFromStatus())
                .toStatus(logEntry.getToStatus())
                .changedById(logEntry.getChangedBy().getId())
                .changedByName(logEntry.getChangedBy().getName())
                .notes(logEntry.getNotes())
                .changedAt(logEntry.getChangedAt())
                .build();
    }
}
