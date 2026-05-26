package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents the assignment of a support engineer to a ticket.
 * A ticket can have multiple support engineers assigned (team-based).
 * Tracks who assigned, when, and optional notes.
 */
@Entity
@Table(name = "ticket_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"ticket_id", "assigned_to_id"},
                name = "uk_ticket_assignment"
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The ticket being assigned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    /**
     * The support engineer assigned to this ticket.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id", nullable = false)
    private User assignedTo;

    /**
     * The admin/user who made this assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    /**
     * Optional notes about the assignment.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * When this assignment was created.
     */
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    /**
     * Whether this assignment is still active.
     * Set to false when unassigned/reassigned.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * When this assignment was deactivated (unassigned).
     */
    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;
}
