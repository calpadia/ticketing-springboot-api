package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the service catalog entry for a client.
 * Each client has at most one service catalog entry that defines which
 * maintenance services (PM and/or CM) are offered to that client.
 *
 * <p>This is independent from {@link ClientQuota} which tracks per-year
 * usage limits — a service catalog entry simply declares <em>which</em>
 * services are part of the agreement.</p>
 */
@Entity
@Table(name = "service_catalogs",
        uniqueConstraints = @UniqueConstraint(
                columnNames = "client_id",
                name = "uk_service_catalog_client"
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The client this catalog entry belongs to. One catalog per client.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    /**
     * The maintenance services offered to this client (PM, CM, or both).
     * Stored as strings in a side table {@code service_catalog_services}.
     */
    @ElementCollection(targetClass = MaintenanceType.class, fetch = FetchType.EAGER)
    @CollectionTable(
            name = "service_catalog_services",
            joinColumns = @JoinColumn(name = "service_catalog_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(
                    columnNames = {"service_catalog_id", "service"},
                    name = "uk_service_catalog_service"
            )
    )
    @Column(name = "service", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<MaintenanceType> services = new HashSet<>();

    /**
     * Optional notes about the service agreement.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
