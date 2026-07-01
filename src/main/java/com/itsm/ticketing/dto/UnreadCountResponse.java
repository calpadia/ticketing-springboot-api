package com.itsm.ticketing.dto;

import lombok.*;

import java.util.List;

/**
 * Response DTO for the unread notification count endpoint.
 * Returned by {@code GET /api/v1/notifications/unread-count}.
 *
 * <p>In addition to the aggregate counts, the response now includes a
 * {@code details} list so the frontend can render a notification dropdown
 * showing exactly which tickets have unread messages.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountResponse {

    /**
     * Number of new tickets the user has not yet opened.
     */
    private long unreadTickets;

    /**
     * Number of new chat messages (from others) sent after the user
     * last opened the respective ticket.
     */
    private long unreadMessages;

    /**
     * Total unread count (unreadTickets + unreadMessages).
     * Convenience field for the frontend badge.
     */
    private long total;

    /**
     * Per-ticket breakdown of unread messages.
     * Each entry represents one ticket that has at least one unread message,
     * showing the latest unread message preview and the ticket reference.
     * Ordered by the latest unread message timestamp descending.
     */
    private List<UnreadNotificationDetail> details;
}
