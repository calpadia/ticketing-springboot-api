package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ClientSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ClientSupport entity.
 * Manages the many-to-many relationship between clients and support users.
 */
@Repository
public interface ClientSupportRepository extends JpaRepository<ClientSupport, Long> {

    /**
     * Find all active support assignments for a client.
     */
    List<ClientSupport> findByClientIdAndActiveTrue(Long clientId);

    /**
     * Find all active client assignments for a support user.
     */
    List<ClientSupport> findBySupportUserIdAndActiveTrue(Long supportUserId);

    /**
     * Find a specific active client-support relationship.
     */
    Optional<ClientSupport> findByClientIdAndSupportUserIdAndActiveTrue(Long clientId, Long supportUserId);

    /**
     * Check if a support user is already assigned to a client (active).
     */
    boolean existsByClientIdAndSupportUserIdAndActiveTrue(Long clientId, Long supportUserId);
}
