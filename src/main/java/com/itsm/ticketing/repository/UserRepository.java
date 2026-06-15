package com.itsm.ticketing.repository;

import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Find users by a set of roles. Used to populate assignable engineers
     * (e.g. SUPPORT and TECHNICAL_SUPPORT) for ticket assignment dropdowns.
     */
    List<User> findByRoleIn(List<Role> roles);
}
