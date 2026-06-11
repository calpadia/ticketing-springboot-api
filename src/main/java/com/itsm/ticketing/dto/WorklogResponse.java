package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a ticket worklog entry.
 * isRunning = true when stoppedAt is null (timer still active).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorklogResponse {

    private Long id;
    private Long ticketId;
    private String ticketNumber;

    // User info (flattened)
    private Long userId;
    private String userName;

    /**
     * Human-readable role label for display in the UI.
     * e.g. "Admin", "Support", "Technical Support", "User"
     */
    private String userRoleLabel;

    private String taskNotes;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private Long loggedDurationSeconds;

    /**
     * Convenience flag: true when this worklog timer is still running.
     * Derived from stoppedAt == null.
     */
    private boolean isRunning;
}

