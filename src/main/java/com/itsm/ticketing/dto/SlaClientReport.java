package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SLA performance report for a single client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaClientReport {

    private Long clientId;
    private String clientName;
    private long totalTickets;
    private SlaMetric response;
    private SlaMetric resolution;
    private List<SlaPriorityBreakdown> priorityBreakdown;
}
