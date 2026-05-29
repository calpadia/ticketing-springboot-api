package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Search tickets with optional filters. Any null parameter is ignored.
     * Used by the CSV export feature.
     *
     * @param clientId optional client ID filter
     * @param ticketIds optional whitelist of ticket IDs (used to scope SUPPORT/TECHNICAL_SUPPORT to assigned tickets);
     *                  pass null to ignore. Pass empty list to match nothing.
     * @param from     optional minimum createdAt (inclusive)
     * @param to       optional maximum createdAt (inclusive)
     */
    @Query("""
            SELECT t FROM Ticket t
            WHERE (:clientId IS NULL OR t.client.id = :clientId)
              AND (:hasIdFilter = false OR t.id IN :ticketIds)
              AND (:from IS NULL OR t.createdAt >= :from)
              AND (:to   IS NULL OR t.createdAt <= :to)
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> searchTicketsForExport(
            @Param("clientId") Long clientId,
            @Param("hasIdFilter") boolean hasIdFilter,
            @Param("ticketIds") List<Long> ticketIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
