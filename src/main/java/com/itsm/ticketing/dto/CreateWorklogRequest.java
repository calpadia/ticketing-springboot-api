package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a new worklog (timer start).
 * By default the userId is resolved from @AuthenticationPrincipal (the caller).
 * Set targetUserId to start a timer on behalf of another user (e.g. SUPPORT assigning TECHNICAL_SUPPORT).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWorklogRequest {

    /**
     * Optional task notes describing the planned work for this session.
     */
    private String taskNotes;

    /**
     * Optional: ID of the user who will actually work on this task.
     * If null, defaults to the authenticated caller (self-assign).
     * Use this when a SUPPORT user starts a timer on behalf of a TECHNICAL_SUPPORT.
     */
    private Long targetUserId;
}
