package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.ChatMessageRequest;
import com.itsm.ticketing.dto.ChatMessageResponse;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ChatMessageRepository;
import com.itsm.ticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages.
 * Handles sending messages and retrieving chat history for tickets.
 * Enforces access control (client-scoped) and ticket status rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final TicketRepository ticketRepository;

    /**
     * Send a new chat message linked to a ticket.
     * Rules:
     * - USER can only send messages on tickets belonging to their client
     * - Chat is blocked if ticket status is RESOLVED or CLOSED
     *
     * @param request the chat message request containing ticketId and content
     * @param sender  the authenticated user sending the message
     * @return the saved chat message as a response DTO
     */
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, User sender) {
        Ticket ticket = ticketRepository.findById(request.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + request.getTicketId()));

        // Access control: USER can only chat on their client's tickets
        validateTicketAccess(ticket, sender);

        // Block chat if ticket is RESOLVED or CLOSED
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException(
                    "Chat tidak bisa dilanjutkan. Ticket sudah berstatus: " + ticket.getStatus());
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .ticket(ticket)
                .sender(sender)
                .content(request.getContent())
                .senderRole(sender.getRole())
                .build();

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("Chat message saved: id={}, ticketId={}, sender={}",
                saved.getId(), ticket.getId(), sender.getEmail());

        return mapToResponse(saved);
    }

    /**
     * Get all chat messages for a ticket by ticket ID.
     * Access controlled: USER can only see chat from their client's tickets.
     *
     * @param ticketId    the ticket ID
     * @param currentUser the authenticated user
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long ticketId, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with id: " + ticketId));

        validateTicketAccess(ticket, currentUser);

        return chatMessageRepository.findByTicketIdOrderBySentAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all chat messages for a ticket by ticket number.
     * Access controlled: USER can only see chat from their client's tickets.
     *
     * @param ticketNumber the unique ticket number
     * @param currentUser  the authenticated user
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistoryByTicketNumber(String ticketNumber, User currentUser) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));

        validateTicketAccess(ticket, currentUser);

        return chatMessageRepository.findByTicketTicketNumberOrderBySentAtAsc(ticketNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Validates that the current user has access to this ticket's chat.
     * ADMIN: always allowed. USER: only if ticket belongs to their client.
     */
    private void validateTicketAccess(Ticket ticket, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return; // Admin can access all ticket chats
        }
        if (currentUser.getClient() == null ||
                !currentUser.getClient().getId().equals(ticket.getClient().getId())) {
            throw new AccessDeniedException(
                    "Anda tidak memiliki akses ke chat ticket ini");
        }
    }

    private ChatMessageResponse mapToResponse(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .ticketId(chatMessage.getTicket().getId())
                .ticketNumber(chatMessage.getTicket().getTicketNumber())
                .senderId(chatMessage.getSender().getId())
                .senderName(chatMessage.getSender().getName())
                .senderRole(chatMessage.getSenderRole().name())
                .content(chatMessage.getContent())
                .sentAt(chatMessage.getSentAt())
                .build();
    }
}
