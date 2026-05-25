package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.ChatMessageRequest;
import com.itsm.ticketing.dto.ChatMessageResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller for chat functionality.
 * Provides WebSocket endpoints for real-time messaging
 * and REST endpoints for retrieving chat history.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    // ========================================================================
    // WebSocket Endpoint (Real-time)
    // ========================================================================

    /**
     * Handle incoming chat messages via WebSocket STOMP.
     * Client sends to: /app/chat.send
     * Server broadcasts to: /topic/chat/{ticketId}
     *
     * @param request   the chat message payload
     * @param principal the authenticated user (from WebSocket session)
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        log.info("WebSocket message received from {} for ticket {}",
                principal.getName(), request.getTicketId());

        // The principal is set by WebSocketAuthInterceptor during CONNECT
        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        ChatMessageResponse response = chatService.sendMessage(request, sender);

        // Broadcast to all subscribers of this ticket's chat topic
        String destination = "/topic/chat/" + request.getTicketId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("Message broadcast to {}", destination);
    }

    // ========================================================================
    // REST Endpoints (Chat History)
    // ========================================================================

    /**
     * Get chat history for a ticket by its ID.
     *
     * @param ticketId the ticket ID
     * @return list of chat messages ordered by sent time
     */
    @GetMapping("/api/v1/chat/{ticketId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long ticketId) {
        log.info("GET /api/v1/chat/{} - Fetching chat history", ticketId);
        List<ChatMessageResponse> history = chatService.getChatHistory(ticketId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get chat history for a ticket by its ticket number.
     *
     * @param ticketNumber the unique ticket number (e.g., TKT-20260522-001)
     * @return list of chat messages ordered by sent time
     */
    @GetMapping("/api/v1/chat/ticket/{ticketNumber}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistoryByTicketNumber(
            @PathVariable String ticketNumber) {
        log.info("GET /api/v1/chat/ticket/{} - Fetching chat history by ticket number", ticketNumber);
        List<ChatMessageResponse> history = chatService.getChatHistoryByTicketNumber(ticketNumber);
        return ResponseEntity.ok(history);
    }
}
