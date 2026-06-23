package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.UnreadCountResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for notification management.
 *
 * <p>Exposes two endpoints that replace the previous localStorage-based
 * unread tracking in the frontend:</p>
 *
 * <ul>
 *   <li>{@code GET /api/v1/notifications/unread-count} — returns the number of
 *       unread tickets and messages for the currently authenticated user. Called
 *       on login, page refresh, and after receiving a WebSocket notification.</li>
 *   <li>{@code POST /api/v1/tickets/{ticketId}/read} — marks a ticket as read.
 *       Called when the user opens the ticket detail page. Upserts the
 *       {@code last_read_at} watermark in the database.</li>
 * </ul>
 *
 * <p>Both endpoints are accessible by all authenticated roles
 * (ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER).</p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get the total unread count for the current user.
     *
     * <p>Returns separate counts for unread tickets (new tickets not yet opened)
     * and unread messages (new chat messages in tickets the user has already opened),
     * plus a combined {@code total} for convenience.</p>
     *
     * @param currentUser the authenticated user (injected from JWT)
     * @return unread counts scoped to the user's accessible tickets
     */
    @GetMapping("/api/v1/notifications/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/notifications/unread-count - User: {}", currentUser.getEmail());
        UnreadCountResponse response = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark a ticket as read for the current user.
     *
     * <p>Triggered when the user opens the ticket detail page. Upserts a
     * {@code ticket_user_reads} record with {@code last_read_at = now()},
     * advancing the read watermark and resetting the unread badge for this ticket.</p>
     *
     * @param ticketId    the ticket to mark as read
     * @param currentUser the authenticated user
     * @return 200 OK on success
     */
    @PostMapping("/api/v1/tickets/{ticketId}/read")
    public ResponseEntity<Void> markTicketAsRead(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User currentUser) {
        log.info("POST /api/v1/tickets/{}/read - User: {}", ticketId, currentUser.getEmail());
        notificationService.markTicketAsRead(ticketId, currentUser);
        return ResponseEntity.ok().build();
    }
}
