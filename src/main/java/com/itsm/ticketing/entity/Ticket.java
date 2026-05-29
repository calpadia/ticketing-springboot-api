package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a support ticket in the ITSM system.
 * Tickets are tied to a client, a requester (user), and track maintenance type quotas.
 */
@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true)
    private String ticketNumber;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Priority is required")
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false)
    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * The project this ticket is associated with (optional).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the first reply from the support team (ADMIN/SUPPORT/TECHNICAL_SUPPORT).
     * Set automatically when the first non-customer chat message is sent.
     * Used to compute the SLA response time.
     */
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    /**
     * Timestamp when the ticket reached the {@code RESOLVED} status for the first time.
     * Set automatically by status update logic. Not cleared on reopen — represents
     * the original resolution timestamp for SLA reporting.
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
