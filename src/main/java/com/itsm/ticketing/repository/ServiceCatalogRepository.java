package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.ServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link ServiceCatalog}.
 * Each client has at most one service catalog entry.
 */
@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {

    Optional<ServiceCatalog> findByClientId(Long clientId);

    boolean existsByClientId(Long clientId);
}
