package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.TicketAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TicketAssignment entity.
 */
@Repository
public interface TicketAssignmentRepository extends JpaRepository<TicketAssignment, Long> {

    /**
     * Find all active assignments for a ticket.
     */
    List<TicketAssignment> findByTicketIdAndActiveTrue(Long ticketId);

    /**
     * Find all active assignments for a support user.
     */
    List<TicketAssignment> findByAssignedToIdAndActiveTrue(Long userId);

    /**
     * Find a specific active assignment (ticket + user).
     */
    Optional<TicketAssignment> findByTicketIdAndAssignedToIdAndActiveTrue(Long ticketId, Long userId);

    /**
     * Check if a user is already assigned to a ticket (active).
     */
    boolean existsByTicketIdAndAssignedToIdAndActiveTrue(Long ticketId, Long userId);

    /**
     * Count active assignments for a ticket.
     */
    long countByTicketIdAndActiveTrue(Long ticketId);
}
