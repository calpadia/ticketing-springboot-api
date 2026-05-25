package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketProgressLogResponse;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.dto.UpdateTicketStatusRequest;
import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
import com.itsm.ticketing.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for managing tickets.
 * Exposes CRUD endpoints for the ticketing system.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {

    private final TicketService ticketService;

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
     *
     * @param id      the ticket ID
     * @param request the status update request
     * @return the updated ticket
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
     * Get the progress history (status change log) of a ticket.
     *
     * @param id the ticket ID
     * @return list of progress log entries
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<List<TicketProgressLogResponse>> getTicketProgress(
            @PathVariable Long id) {
        log.info("GET /api/v1/tickets/{}/progress - Fetching progress logs", id);
        List<TicketProgressLogResponse> logs = ticketService.getProgressLogs(id);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get all tickets.
     */
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        log.info("GET /api/v1/tickets - Fetching all tickets");
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a ticket by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /api/v1/tickets/{} - Fetching ticket by ID", id);
        TicketResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a ticket by its ticket number.
     */
    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(
            @PathVariable String ticketNumber) {
        log.info("GET /api/v1/tickets/number/{} - Fetching ticket by number", ticketNumber);
        TicketResponse response = ticketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(response);
    }
}
