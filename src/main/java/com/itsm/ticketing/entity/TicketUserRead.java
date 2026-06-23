package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Read receipt tracking when a user last opened a specific ticket.
 *
 * <p>Table: {@code ticket_user_reads} — composite PK (ticket_id, user_id).</p>
 *
 * <p>Unread calculation logic:</p>
 * <ul>
 *   <li>If no row exists for (ticket_id, user_id) → ticket/chat is unread</li>
 *   <li>If {@code last_read_at} is older than a new message/ticket → unread</li>
 *   <li>If user is the sender of a message → excluded from unread count</li>
 * </ul>
 *
 * <p>Updated (upserted) every time a user opens the ticket detail page
 * via {@code POST /api/v1/tickets/{ticketId}/read}.</p>
 */
@Entity
@Table(name = "ticket_user_reads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketUserRead {

    @EmbeddedId
    private TicketUserReadId id;

    /**
     * Timestamp of the last time the user opened (read) this ticket.
     * Used as a watermark to compute unread count.
     */
    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;
}
