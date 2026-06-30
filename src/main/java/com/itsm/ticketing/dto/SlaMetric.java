package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated SLA performance metric (response or resolution).
 * Counts and percentages relative to the total set of tickets evaluated.
 *
 * <p>The {@code metTickets}, {@code missedTickets}, and {@code pendingTickets}
 * lists carry lightweight {@link TicketSlaRef} objects so the frontend can
 * drill down into the individual tickets behind each count without making
 * additional API calls.</p>
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

    /** Lightweight references to tickets that met the SLA — for frontend drill-down. */
    @Builder.Default
    private List<TicketSlaRef> metTickets = new ArrayList<>();

    /** Lightweight references to tickets that missed the SLA — for frontend drill-down. */
    @Builder.Default
    private List<TicketSlaRef> missedTickets = new ArrayList<>();

    /** Lightweight references to tickets still pending SLA adjudication — for frontend drill-down. */
    @Builder.Default
    private List<TicketSlaRef> pendingTickets = new ArrayList<>();
}

