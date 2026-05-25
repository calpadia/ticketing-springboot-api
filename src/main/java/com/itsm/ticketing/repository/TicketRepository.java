package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets belonging to a specific client.
     */
    List<Ticket> findByClientId(Long clientId);

    /**
     * Count all tickets created on the current date (for ticket number generation).
     */
    long countByTicketNumberStartingWith(String prefix);
}
