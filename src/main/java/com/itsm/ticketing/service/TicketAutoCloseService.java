package com.itsm.ticketing.service;

import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.repository.ChatMessageRepository;
import com.itsm.ticketing.repository.TicketProgressLogRepository;
import com.itsm.ticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for automatically closing stale tickets.
 *
 * <p>A ticket is considered <em>stale</em> when:</p>
 * <ul>
 *   <li>Its status is {@link TicketStatus#IN_PROGRESS} or {@link TicketStatus#RESOLVED}, AND</li>
 *   <li>The client (role {@link Role#USER}) has not sent any chat message for the past
 *       {@value #INACTIVITY_DAYS} days.</li>
 * </ul>
 *
 * <p>The inactivity window starts from whichever timestamp is more recent:</p>
 * <ul>
 *   <li>{@code resolvedAt} (if status is RESOLVED), or {@code createdAt} (if status is IN_PROGRESS)</li>
 *   <li>The timestamp of the client's most recent chat message after that anchor, if any</li>
 * </ul>
 *
 * <p>Called by {@link com.itsm.ticketing.scheduler.TicketAutoCloseScheduler} every midnight.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketAutoCloseService {

    /** Number of calendar days without a client response before a ticket is auto-closed. */
    static final int INACTIVITY_DAYS = 3;

    private final TicketRepository ticketRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TicketProgressLogRepository progressLogRepository;

    /**
     * Scans all IN_PROGRESS and RESOLVED tickets and closes any that have had
     * no client response for {@value #INACTIVITY_DAYS} days.
     *
     * <p>Each auto-closed ticket receives a {@link TicketProgressLog} entry with
     * {@code changedBy = null} to indicate a system-initiated change.</p>
     *
     * @return the number of tickets that were closed in this run
     */
    @Transactional
    public int processAutoClose() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusDays(INACTIVITY_DAYS);

        List<Ticket> candidates = ticketRepository.findByStatusIn(
                List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED));

        log.info("Auto-close: found {} candidate ticket(s) (IN_PROGRESS / RESOLVED)", candidates.size());

        int closedCount = 0;
        for (Ticket ticket : candidates) {
            if (shouldAutoClose(ticket, cutoff)) {
                closeTicket(ticket, now);
                closedCount++;
            }
        }

        log.info("Auto-close: closed {} ticket(s) in this run", closedCount);
        return closedCount;
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Determines whether the given ticket should be auto-closed.
     *
     * <p>Logic:</p>
     * <ol>
     *   <li>Determine the <em>anchor</em> timestamp — {@code resolvedAt} for RESOLVED tickets,
     *       {@code createdAt} for IN_PROGRESS tickets (fallback safety).</li>
     *   <li>Check for the most recent client chat message sent AFTER the anchor.</li>
     *   <li>The <em>reference</em> point = max(anchor, lastClientChatAt).</li>
     *   <li>If reference &lt; cutoff → ticket is stale → should close.</li>
     * </ol>
     *
     * @param ticket the ticket to evaluate
     * @param cutoff {@code now - INACTIVITY_DAYS}; tickets with reference before this are stale
     * @return {@code true} if the ticket should be auto-closed
     */
    private boolean shouldAutoClose(Ticket ticket, LocalDateTime cutoff) {
        // Determine anchor point: prefer resolvedAt, fall back to createdAt
        LocalDateTime anchor = (ticket.getResolvedAt() != null)
                ? ticket.getResolvedAt()
                : ticket.getCreatedAt();

        // If the anchor itself is more recent than cutoff, the ticket is still "fresh"
        if (anchor == null || !anchor.isBefore(cutoff)) {
            return false;
        }

        // Check if the client has replied after the anchor
        LocalDateTime reference = chatMessageRepository
                .findTopByTicketIdAndSenderRoleAndSentAtAfterOrderBySentAtDesc(
                        ticket.getId(), Role.USER, anchor)
                .map(ChatMessage::getSentAt)
                .orElse(anchor); // no client reply → reference stays at anchor

        boolean stale = reference.isBefore(cutoff);

        if (stale) {
            log.debug("Auto-close candidate: ticket {} (status={}, reference={})",
                    ticket.getTicketNumber(), ticket.getStatus(), reference);
        }

        return stale;
    }

    /**
     * Sets the ticket status to CLOSED and records a system progress log entry.
     *
     * @param ticket the ticket to close
     * @param now    the current timestamp (used for the log entry)
     */
    private void closeTicket(Ticket ticket, LocalDateTime now) {
        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.CLOSED);
        ticketRepository.save(ticket);

        // Record in progress log — changedBy is null to indicate system action
        TicketProgressLog log = TicketProgressLog.builder()
                .ticket(ticket)
                .fromStatus(previousStatus)
                .toStatus(TicketStatus.CLOSED)
                .changedBy(null)
                .notes("Auto-closed: tidak ada respons dari client selama "
                        + INACTIVITY_DAYS + " hari.")
                .build();
        progressLogRepository.save(log);

        this.log.info("Auto-closed ticket {} (was {})", ticket.getTicketNumber(), previousStatus);
    }
}
