package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketProgressLogResponse;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.dto.UpdateTicketPriorityRequest;
import com.itsm.ticketing.dto.UpdateTicketStatusRequest;
import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
import com.itsm.ticketing.entity.ProductType;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.TicketExportService;
import com.itsm.ticketing.service.TicketService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for managing tickets.
 * Exposes CRUD endpoints with access control based on user's client.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;
    private final TicketExportService ticketExportService;

    /**
     * Create a new ticket with optional file attachments.
     * Accepts multipart/form-data to support file uploads.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketResponse> createTicket(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("priority") Priority priority,
            @RequestParam("maintenanceType") MaintenanceType maintenanceType,
            @RequestParam(value = "productType", required = false) ProductType productType,
            @RequestParam(value = "projectId", required = false) Long projectId,
            @RequestParam("clientId") Long clientId,
            @RequestParam("requesterId") Long requesterId,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        log.info("POST /api/v1/tickets - Creating new ticket with {} attachment(s)",
                files != null ? files.size() : 0);

        CreateTicketRequest request = CreateTicketRequest.builder()
                .title(title)
                .description(description)
                .priority(priority)
                .maintenanceType(maintenanceType)
                .productType(productType)
                .projectId(projectId)
                .clientId(clientId)
                .requesterId(requesterId)
                .build();

        TicketResponse response = ticketService.createTicket(request, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create a new ticket without attachments (JSON body).
     * Kept for backward compatibility with existing API consumers.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketResponse> createTicketJson(
            @RequestBody CreateTicketRequest request) {
        log.info("POST /api/v1/tickets (JSON) - Creating new ticket without attachments");
        TicketResponse response = ticketService.createTicket(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update the status of a ticket.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        log.info("PUT /api/v1/tickets/{}/status - Updating status to {}", id, request.getStatus());
        TicketResponse response = ticketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the priority (ticket level) of a ticket.
     * Allowed roles: SUPPORT, ADMIN.
     * Blocked if ticket is already CLOSED or RESOLVED.
     */
    @PutMapping("/{id}/priority")
    public ResponseEntity<TicketResponse> updateTicketPriority(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketPriorityRequest request) {
        log.info("PUT /api/v1/tickets/{}/priority - Updating priority to {}", id, request.getPriority());
        TicketResponse response = ticketService.updateTicketPriority(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the progress history (status change log) of a ticket.
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<List<TicketProgressLogResponse>> getTicketProgress(
            @PathVariable Long id) {
        log.info("GET /api/v1/tickets/{}/progress - Fetching progress logs", id);
        List<TicketProgressLogResponse> logs = ticketService.getProgressLogs(id);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all tickets visible to the current user.
     * ADMIN sees all tickets, USER sees only their client's tickets.
     */
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets(
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/tickets - Fetching tickets for user {} (role: {})",
                currentUser.getEmail(), currentUser.getRole());
        List<TicketResponse> tickets = ticketService.getAllTickets(currentUser);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a ticket by its ID. Access controlled per user's client.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/tickets/{} - Fetching ticket by ID", id);
        TicketResponse response = ticketService.getTicketById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a ticket by its ticket number. Access controlled per user's client.
     */
    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(
            @PathVariable String ticketNumber,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/tickets/number/{} - Fetching ticket by number", ticketNumber);
        TicketResponse response = ticketService.getTicketByNumber(ticketNumber, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Export filtered tickets as CSV.
     * <p>Filters (all optional):</p>
     * <ul>
     *     <li>{@code clientId} — restrict to one client</li>
     *     <li>{@code from} — yyyy-MM-dd, inclusive lower bound on createdAt</li>
     *     <li>{@code to} — yyyy-MM-dd, inclusive upper bound on createdAt</li>
     * </ul>
     * <p>Access control is applied automatically based on caller's role
     * (see {@link TicketExportService}).</p>
     */
    @GetMapping("/export/csv")
    public void exportTicketsCsv(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User currentUser,
            HttpServletResponse response) throws IOException {
        log.info("GET /api/v1/tickets/export/csv - User: {} (clientId={}, from={}, to={})",
                currentUser.getEmail(), clientId, from, to);

        String filename = ticketExportService.buildFilename(from, to);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");

        ticketExportService.exportTicketsAsCsv(
                currentUser, clientId, from, to, response.getOutputStream());
    }
}
