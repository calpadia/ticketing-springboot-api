package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects belonging to a specific client.
     */
    List<Project> findByClientId(Long clientId);

    /**
     * Find all active projects belonging to a specific client.
     */
    List<Project> findByClientIdAndIsActiveTrue(Long clientId);
}
