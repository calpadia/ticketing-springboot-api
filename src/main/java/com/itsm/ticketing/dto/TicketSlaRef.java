package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight ticket reference used inside {@link SlaMetric} to support
 * drill-down on met / missed / pending SLA counts.
 *
 * <p>Only carries the minimal fields needed by the frontend to render a
 * ticket list without additional fetches (no N+1 risk).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSlaRef {

    /** Primary key of the ticket. */
    private Long id;

    /** Human-readable ticket number, e.g. {@code TKT-20260630-001}. */
    private String ticketNumber;

    /** Short title of the ticket. */
    private String title;
}
