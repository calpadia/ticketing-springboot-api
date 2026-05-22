package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Create a new ticket.
     *
     * @param request the ticket creation request payload
     * @return the created ticket with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        log.info("POST /api/v1/tickets - Creating new ticket");
        TicketResponse response = ticketService.createTicket(request);
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
