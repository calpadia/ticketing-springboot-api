package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.TicketProgressLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TicketProgressLog entity.
 */
@Repository
public interface TicketProgressLogRepository extends JpaRepository<TicketProgressLog, Long> {

    /**
     * Find all progress logs for a given ticket, ordered by changedAt ascending.
     */
    List<TicketProgressLog> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}
