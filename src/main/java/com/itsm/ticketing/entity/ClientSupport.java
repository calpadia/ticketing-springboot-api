package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents the assignment of a SUPPORT user to a Client.
 * Many-to-many relationship: 1 client can have many support engineers,
 * and 1 support engineer can serve multiple clients.
 *
 * When a ticket is created for a client, all active support engineers
 * assigned to that client will be auto-assigned to the ticket.
 */
@Entity
@Table(name = "client_supports",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"client_id", "support_user_id"},
                name = "uk_client_support"
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The client this support is assigned to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * The support user assigned to this client.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "support_user_id", nullable = false)
    private User supportUser;

    /**
     * When this assignment was created.
     */
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /**
     * Whether this client-support relationship is active.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
