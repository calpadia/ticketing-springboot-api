package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Priority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-priority SLA breakdown for a single client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaPriorityBreakdown {

    private Priority priority;
    private long totalTickets;
    private SlaMetric response;
    private SlaMetric resolution;
}
