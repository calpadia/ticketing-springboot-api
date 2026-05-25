package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
import com.itsm.ticketing.service.TicketService;
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
     *
     * @param title           ticket title
     * @param description     ticket description
     * @param priority        ticket priority (L1, L2, L3, L4)
     * @param maintenanceType maintenance type (PM, CM)
     * @param clientId        client ID
     * @param requesterId     requester user ID
     * @param files           optional file attachments
     * @return the created ticket with HTTP 201 status
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
     *
     * @param request the ticket creation request payload
     * @return the created ticket with HTTP 201 status
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TicketResponse> createTicketJson(
            @RequestBody CreateTicketRequest request) {
        log.info("POST /api/v1/tickets (JSON) - Creating new ticket without attachments");
        TicketResponse response = ticketService.createTicket(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all tickets.
     *
     * @return list of all tickets
     */
    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        log.info("GET /api/v1/tickets - Fetching all tickets");
        List<TicketResponse> tickets = ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get a ticket by its ID.
     *
     * @param id the ticket ID
     * @return the ticket details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        log.info("GET /api/v1/tickets/{} - Fetching ticket by ID", id);
        TicketResponse response = ticketService.getTicketById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a ticket by its ticket number.
     *
     * @param ticketNumber the unique ticket number (e.g., TKT-20260522-001)
     * @return the ticket details
     */
    @GetMapping("/number/{ticketNumber}")
    public ResponseEntity<TicketResponse> getTicketByNumber(
            @PathVariable String ticketNumber) {
        log.info("GET /api/v1/tickets/number/{} - Fetching ticket by number", ticketNumber);
        TicketResponse response = ticketService.getTicketByNumber(ticketNumber);
        return ResponseEntity.ok(response);
    }
}
