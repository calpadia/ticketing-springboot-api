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
 * Access controlled: USER can only access their client's ticket chats.
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
     * Chat is blocked if ticket is RESOLVED or CLOSED.
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
     * Access controlled per user's client.
     */
    @GetMapping("/api/v1/chat/{ticketId}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/chat/{} - Fetching chat history", ticketId);
        List<ChatMessageResponse> history = chatService.getChatHistory(ticketId, currentUser);
        return ResponseEntity.ok(history);
    }

    /**
     * Get chat history for a ticket by its ticket number.
     * Access controlled per user's client.
     */
    @GetMapping("/api/v1/chat/ticket/{ticketNumber}")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistoryByTicketNumber(
            @PathVariable String ticketNumber,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/chat/ticket/{} - Fetching chat history by ticket number", ticketNumber);
        List<ChatMessageResponse> history = chatService.getChatHistoryByTicketNumber(ticketNumber, currentUser);
        return ResponseEntity.ok(history);
    }
}
