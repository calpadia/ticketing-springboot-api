package com.itsm.ticketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents an internal worklog entry for a ticket.
 * Used for tracking support engineers' work time (live timer & manual log).
 * A worklog is "running" when stoppedAt is null.
 */
@Entity
@Table(name = "ticket_worklogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketWorklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Optional task notes describing what was done during this worklog session.
     */
    @Column(name = "task_notes", columnDefinition = "TEXT")
    private String taskNotes;

    /**
     * Timestamp when the timer was started (set automatically on creation).
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * Timestamp when the timer was stopped. Null means the timer is still running.
     */
    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    /**
     * Total logged duration in seconds. Set when the timer is stopped.
     * The frontend sends this value (computed from startedAt to stoppedAt).
     */
    @Column(name = "logged_duration_seconds")
    private Long loggedDurationSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
