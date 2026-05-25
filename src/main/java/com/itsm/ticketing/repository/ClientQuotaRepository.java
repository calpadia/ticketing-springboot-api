package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ClientQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Find all quota records for a specific client.
     *
     * @param clientId the client ID
     * @return list of quota records for the client
     */
    List<ClientQuota> findByClientId(Long clientId);
}
