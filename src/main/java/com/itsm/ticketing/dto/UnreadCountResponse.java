package com.itsm.ticketing.dto;

import lombok.*;

/**
 * Response DTO for the unread notification count endpoint.
 * Returned by {@code GET /api/v1/notifications/unread-count}.
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
}
