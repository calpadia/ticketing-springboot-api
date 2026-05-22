package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateTicketRequest;
import com.itsm.ticketing.dto.TicketResponse;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.exception.QuotaExceededException;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientQuotaRepository;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Create a new ticket with quota validation.
     *
     * <p>Business rules:
     * <ul>
     *   <li>Validates that the client and requester exist</li>
     *   <li>Checks the client's maintenance quota for the current year</li>
     *   <li>Increments the used quota counter atomically within the transaction</li>
     *   <li>Generates a unique ticket number in format TKT-YYYYMMDD-XXX</li>
     * </ul>
     *
     * @param request the ticket creation request
     * @return the created ticket response
     * @throws ResourceNotFoundException if client or requester is not found
     * @throws QuotaExceededException    if the client's quota is exhausted
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
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

        log.info("Ticket created successfully: {}", ticketNumber);

        return mapToResponse(savedTicket);
    }

    /**
     * Get all tickets.
     *
     * @return list of all ticket responses
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a ticket by its ID.
     *
     * @param id the ticket ID
     * @return the ticket response
     * @throws ResourceNotFoundException if ticket is not found
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + id));
        return mapToResponse(ticket);
    }

    /**
     * Get a ticket by its ticket number.
     *
     * @param ticketNumber the unique ticket number
     * @return the ticket response
     * @throws ResourceNotFoundException if ticket is not found
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketByNumber(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));
        return mapToResponse(ticket);
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
    private TicketResponse mapToResponse(Ticket ticket) {
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
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
