package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.TicketWorklog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketWorklogRepository extends JpaRepository<TicketWorklog, Long> {

    /**
     * Get all worklogs for a ticket, newest first.
     */
    List<TicketWorklog> findByTicketIdOrderByStartedAtDesc(Long ticketId);

    /**
     * Check if a user already has an active (running) worklog on a ticket.
     * Used to prevent duplicate running timers per user per ticket.
     */
    boolean existsByTicketIdAndUserIdAndStoppedAtIsNull(Long ticketId, Long userId);
}
