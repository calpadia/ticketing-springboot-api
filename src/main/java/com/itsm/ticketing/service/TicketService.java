package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AttachmentResponse;
import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketAssignmentResponse;
import com.itsm.ticketing.dto.TicketProgressLogResponse;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.dto.UpdateTicketPriorityRequest;
import com.itsm.ticketing.dto.UpdateTicketStatusRequest;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.event.TicketEvent;
import com.itsm.ticketing.exception.QuotaExceededException;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientQuotaRepository;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.ProjectRepository;
import com.itsm.ticketing.repository.TicketAssignmentRepository;
import com.itsm.ticketing.repository.TicketProgressLogRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.TicketUserReadRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final TicketProgressLogRepository progressLogRepository;
    private final TicketAssignmentRepository assignmentRepository;
    private final ClientSupportService clientSupportService;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketUserReadRepository ticketUserReadRepository;
    private final EmailService emailService;

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

        // 5. Resolve project (optional)
        Project project = null;
        if (request.getProjectId() != null) {
            project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Project not found with ID: " + request.getProjectId()));
            // Validate project belongs to the same client
            if (!project.getClient().getId().equals(client.getId())) {
                throw new IllegalArgumentException(
                        "Project ID " + request.getProjectId() + " tidak milik client " + client.getCompanyName());
            }
        }

        // 6. Build and save the ticket
        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.OPEN)
                .priority(request.getPriority())
                .maintenanceType(request.getMaintenanceType())
                .productType(request.getProductType())
                .client(client)
                .project(project)
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

        // Send email notification to requester
        emailService.sendTicketCreatedEmail(requester.getEmail(), savedTicket);

        // 7. Auto-assign support engineers registered to this client
        autoAssignClientSupports(savedTicket, client);

        log.info("Ticket created successfully: {}", ticketNumber);
        TicketResponse response = mapToResponse(savedTicket, attachmentResponses);

        // Broadcast to WebSocket subscribers after transaction commits
        eventPublisher.publishEvent(new TicketEvent(this, TicketEvent.Type.CREATED, response));

        return response;
    }

    /**
     * Get all tickets visible to the current user.
     * ADMIN: sees all tickets.
     * SUPPORT: sees tickets assigned to them.
     * USER: sees only their client's tickets.
     *
     * @param currentUser the authenticated user
     * @return list of ticket responses
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets(User currentUser) {
        List<Ticket> tickets;

        if (currentUser.getRole() == Role.ADMIN) {
            tickets = ticketRepository.findAllByOrderByCreatedAtDesc();
        } else if (currentUser.getRole() == Role.SUPPORT
                || currentUser.getRole() == Role.TECHNICAL_SUPPORT) {
            // SUPPORT / TECHNICAL_SUPPORT: see tickets assigned to them
            List<Long> assignedTicketIds = assignmentRepository
                    .findByAssignedToIdAndActiveTrue(currentUser.getId())
                    .stream()
                    .map(a -> a.getTicket().getId())
                    .collect(Collectors.toList());
            if (assignedTicketIds.isEmpty()) {
                return Collections.emptyList();
            }
            tickets = ticketRepository.findByIdInOrderByCreatedAtDesc(assignedTicketIds);
        } else {
            // USER: only see tickets from their own client
            if (currentUser.getClient() == null) {
                return Collections.emptyList();
            }
            tickets = ticketRepository.findByClientIdOrderByCreatedAtDesc(currentUser.getClient().getId());
        }

        return tickets.stream()
                .map(ticket -> {
                    List<AttachmentResponse> attachments =
                            fileStorageService.getAttachmentsByTicketId(ticket.getId());
                    return mapToResponse(ticket, attachments, currentUser.getId());
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
        return mapToResponse(ticket, attachments, currentUser.getId());
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
        return mapToResponse(ticket, attachments, currentUser.getId());
    }

    /**
     * Validates that the current user has access to this ticket.
     * ADMIN: always allowed.
     * SUPPORT: allowed if assigned to this ticket.
     * USER: only if the ticket belongs to their client.
     */
    private void validateTicketAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admin can access all tickets
        }
        if (currentUser.getRole() == Role.SUPPORT
                || currentUser.getRole() == Role.TECHNICAL_SUPPORT) {
            // Support / Technical Support can access tickets assigned to them
            if (assignmentRepository.existsByTicketIdAndAssignedToIdAndActiveTrue(
                    ticket.getId(), currentUser.getId())) {
                return;
            }
            throw new org.springframework.security.access.AccessDeniedException(
                    "Anda tidak di-assign ke ticket ini");
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
     * Auto-assign all support engineers registered to a client to a newly created ticket.
     * This ensures that when a USER creates a ticket, all support staff for that client
     * automatically get assigned and can see/work on the ticket.
     */
    private void autoAssignClientSupports(Ticket ticket, Client client) {
        List<User> clientSupports = clientSupportService.getActiveSupportUsersForClient(client.getId());

        if (clientSupports.isEmpty()) {
            log.info("No support engineers registered for client {} - no auto-assignment",
                    client.getCompanyName());
            return;
        }

        for (User supportUser : clientSupports) {
            // Skip if already assigned (shouldn't happen for new tickets, but safety check)
            if (assignmentRepository.existsByTicketIdAndAssignedToIdAndActiveTrue(
                    ticket.getId(), supportUser.getId())) {
                continue;
            }

            TicketAssignment assignment = TicketAssignment.builder()
                    .ticket(ticket)
                    .assignedTo(supportUser)
                    .assignedBy(supportUser) // Auto-assigned by system (use support user as assignedBy)
                    .notes("Auto-assigned: support terdaftar di client " + client.getCompanyName())
                    .active(true)
                    .build();

            assignmentRepository.save(assignment);
        }

        log.info("Auto-assigned {} support engineer(s) to ticket {} (client: {})",
                clientSupports.size(), ticket.getTicketNumber(), client.getCompanyName());
    }

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
     * {@code isRead} is set to null — use the overload with userId for user-facing endpoints.
     */
    private TicketResponse mapToResponse(Ticket ticket, List<AttachmentResponse> attachments) {
        return mapToResponse(ticket, attachments, null);
    }

    /**
     * Maps a Ticket entity to a TicketResponse DTO, computing {@code isRead} for the given user.
     *
     * <p>{@code isRead = true} when a {@link com.itsm.ticketing.entity.TicketUserRead} record
     * exists for (ticketId, userId), meaning the user has opened this ticket detail at least once.
     * {@code isRead = false} when no such record exists — frontend should show the "NEW" badge.</p>
     *
     * @param ticket      the ticket entity
     * @param attachments pre-loaded attachment responses
     * @param userId      the ID of the viewing user; pass null to leave isRead as null
     */
    private TicketResponse mapToResponse(Ticket ticket, List<AttachmentResponse> attachments, Long userId) {
        // Get active assignments for this ticket
        List<TicketAssignmentResponse> assignments = assignmentRepository
                .findByTicketIdAndActiveTrue(ticket.getId())
                .stream()
                .map(this::mapAssignmentToResponse)
                .collect(Collectors.toList());

        // Compute isRead: true if the viewing user has ever opened this ticket detail page
        Boolean isRead = (userId != null)
                ? ticketUserReadRepository.existsByIdTicketIdAndIdUserId(ticket.getId(), userId)
                : null;

        // Compute unreadMessageCount: messages sent by others after this user's last read
        Long unreadMessageCount = (userId != null)
                ? ticketUserReadRepository.countUnreadMessagesByTicketAndUser(ticket.getId(), userId)
                : null;

        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .maintenanceType(ticket.getMaintenanceType())
                .productType(ticket.getProductType())
                .clientId(ticket.getClient().getId())
                .clientCompanyName(ticket.getClient().getCompanyName())
                .requesterId(ticket.getRequester().getId())
                .requesterName(ticket.getRequester().getName())
                .attachments(attachments)
                .assignments(assignments)
                .createdAt(ticket.getCreatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedById(ticket.getClosedBy() != null ? ticket.getClosedBy().getId() : null)
                .closedByName(ticket.getClosedBy() != null ? ticket.getClosedBy().getName() : null)
                .closedAt(ticket.getClosedAt())
                .resolutionSummary(ticket.getResolutionSummary())
                .handlingTimeMinutes(ticket.getHandlingTimeMinutes())
                .isRead(isRead)
                .unreadMessageCount(unreadMessageCount);

        // Include project info if available
        if (ticket.getProject() != null) {
            builder.projectId(ticket.getProject().getId())
                    .projectName(ticket.getProject().getProjectName());
        }

        return builder.build();
    }

    /**
     * Maps a TicketAssignment entity to response DTO.
     */
    private TicketAssignmentResponse mapAssignmentToResponse(TicketAssignment assignment) {
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

        // SLA tracking: stamp resolvedAt the first time we hit RESOLVED.
        // Don't overwrite on reopen+re-resolve — keep the original resolution time.
        if (newStatus == TicketStatus.RESOLVED && ticket.getResolvedAt() == null) {
            ticket.setResolvedAt(java.time.LocalDateTime.now());
            log.info("SLA: resolvedAt set for ticket {}", ticket.getTicketNumber());
        }

        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            if (request.getResolutionSummary() != null && !request.getResolutionSummary().isBlank()) {
                ticket.setResolutionSummary(request.getResolutionSummary());
            }
            if (newStatus == TicketStatus.CLOSED && ticket.getClosedAt() == null) {
                ticket.setClosedAt(java.time.LocalDateTime.now());
                ticket.setClosedBy(changedBy);
            }
            
            // Calculate handling time if we have a resolved/closed timestamp
            java.time.LocalDateTime endStamp = ticket.getResolvedAt() != null ? ticket.getResolvedAt() : ticket.getClosedAt();
            if (ticket.getCreatedAt() != null && endStamp != null && ticket.getHandlingTimeMinutes() == null) {
                long minutes = java.time.Duration.between(ticket.getCreatedAt(), endStamp).toMinutes();
                ticket.setHandlingTimeMinutes(minutes);
            }
        }

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
        TicketResponse response = mapToResponse(updatedTicket, attachments);

        // Broadcast to WebSocket subscribers after transaction commits
        eventPublisher.publishEvent(
                new TicketEvent(this, TicketEvent.Type.STATUS_CHANGED, response));

        return response;
    }

    /**
     * Update the priority (ticket level) of a ticket and record the change in the progress log.
     * <p>
     * Business rules:
     * <ul>
     *   <li>Ticket must NOT be CLOSED or RESOLVED</li>
     *   <li>The requesting user (changedBy) must exist</li>
     *   <li>Change is persisted to {@code ticket_progress_logs} using the notes field for audit trail</li>
     * </ul>
     *
     * @param ticketId the ticket ID
     * @param request  the priority update request
     * @return the updated ticket response
     */
    @Transactional
    public TicketResponse updateTicketPriority(Long ticketId, UpdateTicketPriorityRequest request) {
        log.info("Updating ticket {} priority to {}", ticketId, request.getPriority());

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Tidak dapat mengubah level tiket yang sudah selesai atau ditutup. Status saat ini: "
                            + ticket.getStatus());
        }

        User changedBy = userRepository.findById(request.getChangedBy())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + request.getChangedBy()));

        Priority oldPriority = ticket.getPriority();
        Priority newPriority = request.getPriority();

        if (oldPriority == newPriority) {
            throw new IllegalArgumentException(
                    "Ticket sudah berada di level " + oldPriority + ". Tidak ada perubahan.");
        }

        ticket.setPriority(newPriority);
        Ticket updatedTicket = ticketRepository.save(ticket);

        // Audit trail: embed priority change in notes field of TicketProgressLog
        String auditNotes = String.format("[PRIORITY CHANGE] %s -> %s", oldPriority, newPriority);
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            auditNotes += " | " + request.getNotes().trim();
        }

        TicketProgressLog progressLog = TicketProgressLog.builder()
                .ticket(ticket)
                .fromStatus(ticket.getStatus())
                .toStatus(ticket.getStatus())
                .changedBy(changedBy)
                .notes(auditNotes)
                .build();
        progressLogRepository.save(progressLog);

        log.info("Ticket {} priority updated: {} -> {} by user {}",
                ticket.getTicketNumber(), oldPriority, newPriority, changedBy.getName());

        List<AttachmentResponse> attachments =
                fileStorageService.getAttachmentsByTicketId(ticketId);
        TicketResponse response = mapToResponse(updatedTicket, attachments);

        // Broadcast priority change event to WebSocket subscribers
        eventPublisher.publishEvent(
                new TicketEvent(this, TicketEvent.Type.STATUS_CHANGED, response));

        return response;
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
