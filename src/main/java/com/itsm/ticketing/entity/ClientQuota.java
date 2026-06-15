package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents the yearly quota allocation for a client.
 * Tracks PM (Preventive Maintenance) and CM (Corrective Maintenance) quotas
 * along with how many have been used.
 */
@Entity
@Table(name = "client_quotas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "pm_quota", nullable = false)
    private Integer pmQuota;

    @Column(name = "cm_quota", nullable = false)
    private Integer cmQuota;

    @Builder.Default
    @Column(name = "pm_used", nullable = false)
    private Integer pmUsed = 0;

    @Builder.Default
    @Column(name = "cm_used", nullable = false)
    private Integer cmUsed = 0;
}
