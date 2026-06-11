package com.itsm.ticketing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for stopping a running worklog.
 * Frontend computes the duration from startedAt to stoppedAt and sends both.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopWorklogRequest {

    @NotNull(message = "stoppedAt is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime stoppedAt;

    @NotNull(message = "loggedDurationSeconds is required")
    private Long loggedDurationSeconds;

    /**
     * Optional updated task notes to replace or append details after completion.
     */
    private String taskNotes;
}
