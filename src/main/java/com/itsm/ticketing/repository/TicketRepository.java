package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Count all tickets created on the current date (for ticket number generation).
     * Uses a native-like query derived method to count today's tickets.
     */
    long countByTicketNumberStartingWith(String prefix);
}
