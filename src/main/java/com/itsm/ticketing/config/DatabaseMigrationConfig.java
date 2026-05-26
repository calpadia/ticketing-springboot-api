package com.itsm.ticketing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * One-time database migration to update the role CHECK constraint.
 * Updates from old enum values (USER, AGENT, MANAGER) to new values (ADMIN, USER).
 * This can be safely removed after the migration has been applied.
 */
@Configuration
@Slf4j
public class DatabaseMigrationConfig {

    @Bean
    public CommandLineRunner migrateRoleConstraint(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                log.info("Checking and updating role constraint...");

                // Drop old constraint
                stmt.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");

                // Add new constraint with updated role values
                stmt.execute("ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('ADMIN', 'SUPPORT', 'TECHNICAL_SUPPORT', 'USER'))");

                log.info("Role constraint updated successfully: ADMIN, SUPPORT, TECHNICAL_SUPPORT, USER");

            } catch (Exception e) {
                log.warn("Role constraint migration skipped or already applied: {}", e.getMessage());
            }
        };
    }
}
