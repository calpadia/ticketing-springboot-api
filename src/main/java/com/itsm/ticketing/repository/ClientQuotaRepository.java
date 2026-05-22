package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ClientQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientQuotaRepository extends JpaRepository<ClientQuota, Long> {

    /**
     * Find the quota record for a specific client and year.
     *
     * @param clientId the client ID
     * @param year     the quota year
     * @return the quota record if found
     */
    Optional<ClientQuota> findByClientIdAndYear(Long clientId, Integer year);
}
