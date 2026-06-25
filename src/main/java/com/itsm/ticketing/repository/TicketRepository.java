package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>,
        JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets belonging to a specific client.
     */
    List<Ticket> findByClientId(Long clientId);

    /**
     * Count all tickets created on the current date (for ticket number generation).
     */
    long countByTicketNumberStartingWith(String prefix);

    /**
     * Find all tickets whose status is in the given set.
     * Used by the auto-close scheduler to retrieve IN_PROGRESS and RESOLVED candidates.
     *
     * @param statuses collection of statuses to match
     * @return list of matching tickets
     */
    List<Ticket> findByStatusIn(List<TicketStatus> statuses);
}
