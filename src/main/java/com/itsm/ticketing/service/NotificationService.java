package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.UnreadCountResponse;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.TicketAssignmentRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.TicketUserReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for database-driven notification management.
 *
 * <p>Provides two core operations:</p>
 * <ol>
 *   <li>{@link #getUnreadCount(User)} — computes the unread ticket and message
 *       counts for the currently authenticated user, scoped to their accessible tickets.</li>
 *   <li>{@link #markTicketAsRead(Long, User)} — upserts a {@link TicketUserRead}
 *       record so the user's read watermark is advanced to {@code now()},
 *       effectively clearing the unread state for that ticket.</li>
 * </ol>
 *
 * <p>This service replaces the frontend localStorage approach, enabling
 * cross-device synchronisation and eliminating cross-account state pollution.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TicketUserReadRepository ticketUserReadRepository;
    private final TicketRepository ticketRepository;
    private final TicketAssignmentRepository assignmentRepository;

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    /**
     * Compute the unread count for the current user.
     *
     * <p>Access scoping mirrors {@link TicketService#getAllTickets(User)}:</p>
     * <ul>
     *   <li>ADMIN — all tickets</li>
     *   <li>SUPPORT / TECHNICAL_SUPPORT — tickets actively assigned to them</li>
     *   <li>USER — tickets belonging to their client</li>
     * </ul>
     *
     * @param currentUser the authenticated user
     * @return response with unreadTickets, unreadMessages, and total
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(User currentUser) {
        List<Long> accessibleTicketIds = resolveAccessibleTicketIds(currentUser);

        if (accessibleTicketIds.isEmpty()) {
            log.debug("Notification count: user {} has no accessible tickets", currentUser.getEmail());
            return UnreadCountResponse.builder()
                    .unreadTickets(0)
                    .unreadMessages(0)
                    .total(0)
                    .build();
        }

        long unreadTickets = ticketUserReadRepository
                .countUnreadTickets(currentUser.getId(), accessibleTicketIds);

        long unreadMessages = ticketUserReadRepository
                .countUnreadMessages(currentUser.getId(), accessibleTicketIds);

        long total = unreadTickets + unreadMessages;

        log.debug("Notification count for user {}: tickets={}, messages={}, total={}",
                currentUser.getEmail(), unreadTickets, unreadMessages, total);

        return UnreadCountResponse.builder()
                .unreadTickets(unreadTickets)
                .unreadMessages(unreadMessages)
                .total(total)
                .build();
    }

    /**
     * Mark a ticket as read for the current user by upserting a
     * {@link TicketUserRead} record with {@code lastReadAt = now()}.
     *
     * <p>Called when the user opens the ticket detail page. Advances the
     * read watermark so new messages/tickets since this moment are counted
     * as unread going forward.</p>
     *
     * @param ticketId    the ticket to mark as read
     * @param currentUser the authenticated user
     * @throws ResourceNotFoundException if the ticket does not exist
     * @throws AccessDeniedException     if the user cannot access this ticket
     */
    @Transactional
    public void markTicketAsRead(Long ticketId, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with ID: " + ticketId));

        validateTicketAccess(ticket, currentUser);

        TicketUserReadId readId = new TicketUserReadId(ticketId, currentUser.getId());

        // Upsert: find existing record or create new one
        TicketUserRead readReceipt = ticketUserReadRepository.findById(readId)
                .orElse(TicketUserRead.builder().id(readId).build());

        readReceipt.setLastReadAt(LocalDateTime.now());
        ticketUserReadRepository.save(readReceipt);

        log.debug("Ticket {} marked as read by user {} at {}",
                ticket.getTicketNumber(), currentUser.getEmail(), readReceipt.getLastReadAt());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Returns the list of ticket IDs accessible to the given user,
     * using the same access-control logic as TicketService.
     */
    private List<Long> resolveAccessibleTicketIds(User user) {
        if (user.getRole() == Role.ADMIN) {
            return ticketRepository.findAll()
                    .stream()
                    .map(Ticket::getId)
                    .collect(Collectors.toList());
        }

        if (user.getRole() == Role.SUPPORT || user.getRole() == Role.TECHNICAL_SUPPORT) {
            return assignmentRepository
                    .findByAssignedToIdAndActiveTrue(user.getId())
                    .stream()
                    .map(a -> a.getTicket().getId())
                    .collect(Collectors.toList());
        }

        // USER role
        if (user.getClient() == null) {
            return Collections.emptyList();
        }
        return ticketRepository.findByClientId(user.getClient().getId())
                .stream()
                .map(Ticket::getId)
                .collect(Collectors.toList());
    }

    /**
     * Validates that the current user has access to this ticket.
     * Mirrors the access control in TicketService and ChatService.
     */
    private void validateTicketAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() == Role.SUPPORT
                || currentUser.getRole() == Role.TECHNICAL_SUPPORT) {
            if (assignmentRepository.existsByTicketIdAndAssignedToIdAndActiveTrue(
                    ticket.getId(), currentUser.getId())) {
                return;
            }
            throw new AccessDeniedException("Anda tidak di-assign ke ticket ini");
        }
        if (currentUser.getClient() == null ||
                !currentUser.getClient().getId().equals(ticket.getClient().getId())) {
            throw new AccessDeniedException("Anda tidak memiliki akses ke ticket ini");
        }
    }
}
