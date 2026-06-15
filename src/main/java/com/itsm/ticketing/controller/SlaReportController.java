package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.SlaReportResponse;
import com.itsm.ticketing.dto.SlaTargetResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.SlaReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for SLA performance reports.
 *
 * <ul>
 *   <li>{@code GET /api/v1/sla-report} — full SLA report (targets + per-client metrics)</li>
 *   <li>{@code GET /api/v1/sla-report/targets} — only the SLA target table (cheap call)</li>
 * </ul>
 *
 * <p>Access: ADMIN sees all clients; USER is auto-scoped to their own client.</p>
 */
@RestController
@RequestMapping("/api/v1/sla-report")
@RequiredArgsConstructor
@Slf4j
public class SlaReportController {

    private final SlaReportService slaReportService;

    /**
     * Get the SLA report. All filters are optional.
     *
     * @param clientId optional client filter (ADMIN only; USER is forced to own client)
     * @param from     optional inclusive start date (yyyy-MM-dd)
     * @param to       optional inclusive end date (yyyy-MM-dd)
     */
    @GetMapping
    public ResponseEntity<SlaReportResponse> getSlaReport(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal User caller) {
        log.info("GET /api/v1/sla-report - {} (clientId={}, from={}, to={})",
                caller.getEmail(), clientId, from, to);
        return ResponseEntity.ok(slaReportService.generateReport(caller, clientId, from, to));
    }

    /**
     * Get only the SLA target table.
     */
    @GetMapping("/targets")
    public ResponseEntity<List<SlaTargetResponse>> getSlaTargets() {
        log.info("GET /api/v1/sla-report/targets");
        return ResponseEntity.ok(slaReportService.getSlaTargets());
    }
}
