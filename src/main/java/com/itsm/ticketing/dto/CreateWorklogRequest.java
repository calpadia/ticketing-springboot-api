package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting a new worklog (timer start).
 * userId resolved from @AuthenticationPrincipal in controller.
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
}
