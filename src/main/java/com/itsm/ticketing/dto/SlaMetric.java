package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated SLA performance metric (response or resolution).
 * Counts and percentages relative to the total set of tickets evaluated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaMetric {

    /** Tickets that met the SLA (responded/resolved within target). */
    private long met;

    /** Tickets that missed the SLA (responded/resolved past target, or in-flight beyond target). */
    private long missed;

    /** Tickets still within target window without a response/resolution yet. */
    private long pending;

    /** met / (met + missed) — percentage of finished tickets that met the SLA. 0–100, 2-decimal. */
    private double compliancePercent;

    /** Average actual hours from createdAt to event (response/resolution). Only over tickets with the event. */
    private double averageHours;
}
