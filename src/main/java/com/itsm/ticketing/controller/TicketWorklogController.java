package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateWorklogRequest;
import com.itsm.ticketing.dto.StopWorklogRequest;
import com.itsm.ticketing.dto.WorklogResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.TicketWorklogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing internal ticket worklogs (live timer).
 *
 * <p>Access control:
 * <ul>
 *   <li>POST (start) and PUT (stop): ADMIN, SUPPORT, TECHNICAL_SUPPORT only</li>
 *   <li>GET (list): any authenticated user with access to the ticket</li>
 * </ul>
 * Role enforcement is done in {@link com.itsm.ticketing.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/worklogs")
@RequiredArgsConstructor
@Slf4j
public class TicketWorklogController {

    private final TicketWorklogService worklogService;

    /**
     * Start a new worklog timer on a ticket.
     *
     * <p>The authenticated user is set as the caller. If {@code request.targetUserId} is
     * provided, the worklog is created on behalf of that user (the actual worker).
     * This allows a SUPPORT user to start a timer for a TECHNICAL_SUPPORT.
     */
    @PostMapping
    public ResponseEntity<WorklogResponse> startWorklog(
            @PathVariable Long ticketId,
            @RequestBody(required = false) CreateWorklogRequest request,
            @AuthenticationPrincipal User currentUser) {

        log.info("POST /api/v1/tickets/{}/worklogs - User {} starting worklog (targetUserId={})",
                ticketId, currentUser.getEmail(),
                request != null ? request.getTargetUserId() : null);

        WorklogResponse response = worklogService.startWorklog(ticketId, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all worklogs for a ticket (newest first).
     */
    @GetMapping
    public ResponseEntity<List<WorklogResponse>> getWorklogs(
            @PathVariable Long ticketId) {

        log.info("GET /api/v1/tickets/{}/worklogs - Fetching worklogs", ticketId);
        List<WorklogResponse> worklogs = worklogService.getWorklogsByTicket(ticketId);
        return ResponseEntity.ok(worklogs);
    }

    /**
     * Stop a running worklog timer.
     * Sets stoppedAt and loggedDurationSeconds as provided by the client.
     */
    @PutMapping("/{worklogId}/stop")
    public ResponseEntity<WorklogResponse> stopWorklog(
            @PathVariable Long ticketId,
            @PathVariable Long worklogId,
            @Valid @RequestBody StopWorklogRequest request) {

        log.info("PUT /api/v1/tickets/{}/worklogs/{}/stop - Stopping worklog",
                ticketId, worklogId);

        WorklogResponse response = worklogService.stopWorklog(ticketId, worklogId, request);
        return ResponseEntity.ok(response);
    }
}
