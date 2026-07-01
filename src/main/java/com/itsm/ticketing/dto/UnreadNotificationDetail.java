package com.itsm.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Detail of a single ticket that has unread messages.
 *
 * <p>Included in {@link UnreadCountResponse#getDetails()} so the frontend
 * can render a notification dropdown showing which tickets need attention,
 * who sent the latest message, and a short preview of the message content.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadNotificationDetail {

    /**
     * ID of the ticket with unread messages.
     */
    private Long ticketId;

    /**
     * Human-readable ticket number (e.g. TKT-20260701-003).
     */
    private String ticketNumber;

    /**
     * Name of the client who owns the ticket (used as "sender" label in the UI).
     */
    private String senderName;

    /**
     * Short preview of the latest unread message content (max 80 chars).
     * Truncated with "..." if the original message is longer.
     */
    private String messagePreview;

    /**
     * Timestamp of the latest unread message in this ticket.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
