package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateWorklogRequest;
import com.itsm.ticketing.dto.StopWorklogRequest;
import com.itsm.ticketing.dto.WorklogResponse;
import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.TicketWorklog;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.TicketWorklogRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing internal ticket worklogs (live timer & duration log).
 *
 * <p>Business rules:
 * <ul>
 *   <li>A user can only have ONE active (running) worklog per ticket at a time.</li>
 *   <li>Stopping a worklog requires the worklog to belong to the same ticket.</li>
 *   <li>A worklog that is already stopped cannot be stopped again.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketWorklogService {

    private final TicketWorklogRepository worklogRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * Start a new worklog timer for a user on a ticket.
     *
     * <p>If {@code request.targetUserId} is provided, the worklog is created on behalf of
     * that user (the worker). Otherwise the caller is used as the worker.
     * The active-timer constraint is checked against the resolved worker, not the caller.
     *
     * @param ticketId the ticket ID
     * @param caller   the authenticated user starting the timer
     * @param request  optional task notes and optional targetUserId
     * @return the created worklog response
     */
    @Transactional
    public WorklogResponse startWorklog(Long ticketId, User caller, CreateWorklogRequest request) {
        log.info("Starting worklog on ticket {} by caller {} ({})", ticketId, caller.getName(), caller.getId());

        // Validate ticket exists
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        // Resolve the actual worker — use targetUserId if provided, else the caller
        User worker = caller;
        if (request != null && request.getTargetUserId() != null) {
            worker = userRepository.findById(request.getTargetUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with ID: " + request.getTargetUserId()));
            log.info("Worklog will be created on behalf of user {} ({}) by caller {} ({})",
                    worker.getName(), worker.getId(), caller.getName(), caller.getId());
        }

        // Multiple worklogs can run simultaneously — no constraint on active timers per ticket
        TicketWorklog worklog = TicketWorklog.builder()
                .ticket(ticket)
                .user(worker)   // use resolved worker, not always caller
                .taskNotes(request != null ? request.getTaskNotes() : null)
                .startedAt(LocalDateTime.now())
                .build();

        TicketWorklog saved = worklogRepository.save(worklog);
        log.info("Worklog {} started for ticket {} — worker: {}, caller: {}",
                saved.getId(), ticketId, worker.getName(), caller.getName());

        return mapToResponse(saved);
    }

    /**
     * Stop a running worklog timer.
     *
     * @param ticketId  the ticket ID (for ownership validation)
     * @param worklogId the worklog ID to stop
     * @param request   stop timestamp and computed duration
     * @return the updated worklog response
     */
    @Transactional
    public WorklogResponse stopWorklog(Long ticketId, Long worklogId, StopWorklogRequest request) {
        log.info("Stopping worklog {} on ticket {}", worklogId, ticketId);

        TicketWorklog worklog = worklogRepository.findById(worklogId)
                .orElseThrow(() -> new ResourceNotFoundException("Worklog not found with ID: " + worklogId));

        // Validate worklog belongs to the given ticket
        if (!worklog.getTicket().getId().equals(ticketId)) {
            throw new IllegalArgumentException(
                    "Worklog " + worklogId + " tidak milik ticket " + ticketId);
        }

        // Validate worklog is still running
        if (worklog.getStoppedAt() != null) {
            throw new IllegalStateException(
                    "Worklog " + worklogId + " sudah dihentikan pada " + worklog.getStoppedAt());
        }

        worklog.setStoppedAt(request.getStoppedAt());
        worklog.setLoggedDurationSeconds(request.getLoggedDurationSeconds());

        // Allow updating task notes on stop if provided
        if (request.getTaskNotes() != null && !request.getTaskNotes().isBlank()) {
            worklog.setTaskNotes(request.getTaskNotes());
        }

        TicketWorklog saved = worklogRepository.save(worklog);
        log.info("Worklog {} stopped. Duration: {}s", worklogId, request.getLoggedDurationSeconds());

        return mapToResponse(saved);
    }

    /**
     * Get all worklogs for a ticket, ordered by startedAt descending.
     *
     * @param ticketId the ticket ID
     * @return list of worklog responses
     */
    @Transactional(readOnly = true)
    public List<WorklogResponse> getWorklogsByTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with ID: " + ticketId);
        }
        return worklogRepository.findByTicketIdOrderByStartedAtDesc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private WorklogResponse mapToResponse(TicketWorklog worklog) {
        return WorklogResponse.builder()
                .id(worklog.getId())
                .ticketId(worklog.getTicket().getId())
                .ticketNumber(worklog.getTicket().getTicketNumber())
                .userId(worklog.getUser().getId())
                .userName(worklog.getUser().getName())
                .userRoleLabel(formatRoleLabel(worklog.getUser().getRole()))
                .taskNotes(worklog.getTaskNotes())
                .startedAt(worklog.getStartedAt())
                .stoppedAt(worklog.getStoppedAt())
                .loggedDurationSeconds(worklog.getLoggedDurationSeconds())
                .isRunning(worklog.getStoppedAt() == null)
                .build();
    }

    /**
     * Converts Role enum to a human-readable display label for the frontend UI.
     */
    private String formatRoleLabel(com.itsm.ticketing.entity.Role role) {
        if (role == null) return "";
        return switch (role) {
            case ADMIN -> "Admin";
            case SUPPORT -> "Support";
            case TECHNICAL_SUPPORT -> "Technical Support";
            case USER -> "User";
        };
    }
}
