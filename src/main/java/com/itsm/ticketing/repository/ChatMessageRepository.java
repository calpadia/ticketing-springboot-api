package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatMessage entity.
 * Provides methods to retrieve chat history for a specific ticket.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find all chat messages for a ticket, ordered by sent time ascending.
     */
    List<ChatMessage> findByTicketIdOrderBySentAtAsc(Long ticketId);

    /**
     * Find all chat messages for a ticket by ticket number, ordered by sent time ascending.
     */
    List<ChatMessage> findByTicketTicketNumberOrderBySentAtAsc(String ticketNumber);
}
