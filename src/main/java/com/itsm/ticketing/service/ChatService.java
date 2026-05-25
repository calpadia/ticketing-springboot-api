package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.ChatMessageRequest;
import com.itsm.ticketing.dto.ChatMessageResponse;
import com.itsm.ticketing.entity.ChatMessage;
import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ChatMessageRepository;
import com.itsm.ticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages.
 * Handles sending messages and retrieving chat history for tickets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final TicketRepository ticketRepository;

    /**
     * Send a new chat message linked to a ticket.
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
     *
     * @param ticketId the ticket ID
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistory(Long ticketId) {
        // Verify ticket exists
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket not found with id: " + ticketId);
        }

        return chatMessageRepository.findByTicketIdOrderBySentAtAsc(ticketId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all chat messages for a ticket by ticket number.
     *
     * @param ticketNumber the unique ticket number (e.g., TKT-20260522-001)
     * @return list of chat messages ordered by sent time
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getChatHistoryByTicketNumber(String ticketNumber) {
        // Verify ticket exists
        ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ticket not found with number: " + ticketNumber));

        return chatMessageRepository.findByTicketTicketNumberOrderBySentAtAsc(ticketNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

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
