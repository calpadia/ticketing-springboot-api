package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SLA target hours per priority level.
 * Used by the frontend to render the "SLA Targets" reference card.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaTargetResponse {

    private Priority priority;
    private long responseHours;
    private long resolutionHours;
}
