package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ChatMessage;
import com.itsm.ticketing.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * Find the most recent chat message sent by a specific role for a given ticket
     * AFTER the given timestamp. Used by auto-close scheduler to check whether
     * the client (USER) has replied after a ticket was RESOLVED or entered IN_PROGRESS.
     *
     * @param ticketId  the ticket ID
     * @param role      the sender role to filter by (e.g. {@link Role#USER})
     * @param after     only consider messages sent after this timestamp
     * @return the most recent matching message, or empty if client has not replied
     */
    Optional<ChatMessage> findTopByTicketIdAndSenderRoleAndSentAtAfterOrderBySentAtDesc(
            Long ticketId, Role role, LocalDateTime after);
}
