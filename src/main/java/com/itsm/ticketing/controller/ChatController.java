package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.ChatMessageRequest;
import com.itsm.ticketing.dto.ChatMessageResponse;
import com.itsm.ticketing.dto.ChatUploadResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * Controller for chat functionality.
 * Provides WebSocket endpoints for real-time messaging,
 * REST endpoints for retrieving chat history,
 * and file upload/download endpoints for chat attachments.
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

    /**
     * Handle exceptions thrown from {@link #sendMessage}.
     * Without this, errors are silently swallowed by Spring's WebSocket
     * dispatcher and the user has no idea why the message did not arrive.
     * The error is sent back privately to the originating user via
     * {@code /user/queue/errors} so the frontend can show a toast.
     */
    @org.springframework.messaging.handler.annotation.MessageExceptionHandler
    @org.springframework.messaging.simp.annotation.SendToUser(value = "/queue/errors", broadcast = false)
    public java.util.Map<String, Object> handleChatException(Exception ex) {
        log.warn("Chat WebSocket error: {}", ex.getMessage());
        return java.util.Map.of(
                "error", ex.getClass().getSimpleName(),
                "message", ex.getMessage() != null ? ex.getMessage() : "Internal error"
        );
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

    // ========================================================================
    // REST Endpoints (File Upload & Download)
    // ========================================================================

    /**
     * Upload a file for use in a chat message.
     * The file is stored first, and the returned attachment ID should be
     * included in the WebSocket message's attachmentIds when sending.
     *
     * @param ticketId the ticket this file belongs to
     * @param file     the file to upload
     * @param user     the authenticated user
     * @return upload response with attachment ID
     */
    @PostMapping("/api/v1/chat/upload")
    public ResponseEntity<ChatUploadResponse> uploadChatFile(
            @RequestParam Long ticketId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        log.info("POST /api/v1/chat/upload - User: {} uploading file for ticket: {}",
                user.getEmail(), ticketId);

        ChatUploadResponse response = chatService.uploadChatFile(ticketId, file, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Download a chat attachment by its ID.
     * Access controlled: user must have access to the ticket this attachment belongs to.
     *
     * @param id   the chat attachment ID
     * @param user the authenticated user
     * @return the file resource
     */
    @GetMapping("/api/v1/chat/attachments/{id}/download")
    public ResponseEntity<Resource> downloadChatAttachment(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        log.info("GET /api/v1/chat/attachments/{}/download - User: {} downloading", id, user.getEmail());

        Resource resource = chatService.loadChatAttachmentAsResource(id, user);
        String fileName = chatService.getChatAttachmentFileName(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
