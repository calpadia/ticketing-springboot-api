package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Top-level SLA report response.
 * Contains the SLA targets used (so the frontend doesn't need to hardcode them)
 * plus per-client performance, optionally filtered by date range.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaReportResponse {

    /** SLA target table (per priority). */
    private List<SlaTargetResponse> targets;

    /** Per-client SLA performance. */
    private List<SlaClientReport> clients;

    /** Filter echo. */
    private LocalDate from;
    private LocalDate to;
    private LocalDate generatedAt;
}
