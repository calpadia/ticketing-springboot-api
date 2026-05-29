package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.*;
import com.itsm.ticketing.entity.*;
import com.itsm.ticketing.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Computes SLA performance reports per client.
 *
 * <p>SLA targets are defined per {@link Priority} as a fixed table. They can be
 * promoted to a database-backed config later without changing the API shape.</p>
 *
 * <h3>Definitions</h3>
 * <ul>
 *   <li><b>Response</b>: time from {@code createdAt} until the support team's first
 *       chat message ({@code firstResponseAt}).</li>
 *   <li><b>Resolution</b>: time from {@code createdAt} until status reaches RESOLVED
 *       ({@code resolvedAt}).</li>
 * </ul>
 *
 * <h3>State logic for each metric</h3>
 * <ul>
 *   <li><b>met</b> — event happened within target.</li>
 *   <li><b>missed</b> — event happened past target, OR event has not happened
 *       and elapsed time already exceeded target (in-flight breach).</li>
 *   <li><b>pending</b> — event has not happened and elapsed time is still
 *       within target.</li>
 * </ul>
 *
 * <p>Compliance % is calculated as {@code met / (met + missed)} × 100, ignoring
 * pending tickets (they have not been adjudicated yet).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SlaReportService {

    /**
     * Default SLA targets. Could be moved to a config file or DB later.
     * Hours are wall-clock, not business hours, to keep semantics simple.
     */
    private static final Map<Priority, long[]> SLA_TARGETS_HOURS = Map.of(
            Priority.L1, new long[]{1, 4},     // Critical:  1h response,  4h resolution
            Priority.L2, new long[]{2, 8},     // High:      2h response,  8h resolution
            Priority.L3, new long[]{4, 24},    // Medium:    4h response, 24h resolution
            Priority.L4, new long[]{8, 72}     // Low:       8h response, 72h resolution
    );

    private final TicketRepository ticketRepository;

    // ========================================================================
    // PUBLIC API
    // ========================================================================

    public List<SlaTargetResponse> getSlaTargets() {
        return Arrays.stream(Priority.values())
                .filter(SLA_TARGETS_HOURS::containsKey)
                .map(p -> SlaTargetResponse.builder()
                        .priority(p)
                        .responseHours(SLA_TARGETS_HOURS.get(p)[0])
                        .resolutionHours(SLA_TARGETS_HOURS.get(p)[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Generate an SLA report for the given caller.
     *
     * <p>Filtering rules:</p>
     * <ul>
     *   <li>{@code from} / {@code to} — applied on {@code Ticket.createdAt}.</li>
     *   <li>{@code clientId} — optional filter to a single client.</li>
     *   <li>USER role: {@code clientId} is overridden to the caller's own client.
     *       USER without a client gets an empty report.</li>
     *   <li>Only ADMIN and USER may call this endpoint.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code from} is after {@code to}
     */
    @Transactional(readOnly = true)
    public SlaReportResponse generateReport(
            User caller, Long clientId, LocalDate from, LocalDate to) {

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' tidak boleh setelah 'to'");
        }

        Long effectiveClientId = clientId;
        if (caller.getRole() == Role.USER) {
            if (caller.getClient() == null) {
                log.info("USER {} has no client; returning empty SLA report", caller.getEmail());
                return emptyReport(from, to);
            }
            effectiveClientId = caller.getClient().getId();
        } else if (caller.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(
                    "Role " + caller.getRole() + " tidak dapat mengakses SLA report");
        }

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;

        List<Ticket> tickets = ticketRepository.searchTicketsForExport(
                effectiveClientId,
                false,
                Collections.emptyList(),
                fromDt,
                toDt
        );
        log.info("SLA report: caller={}, clientFilter={}, from={}, to={}, tickets={}",
                caller.getEmail(), effectiveClientId, from, to, tickets.size());

        List<SlaClientReport> clientReports = tickets.stream()
                .filter(t -> t.getClient() != null)
                .collect(Collectors.groupingBy(t -> t.getClient().getId()))
                .values()
                .stream()
                .map(this::buildClientReport)
                .sorted(Comparator.comparing(SlaClientReport::getClientName,
                        Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        return SlaReportResponse.builder()
                .targets(getSlaTargets())
                .clients(clientReports)
                .from(from)
                .to(to)
                .generatedAt(LocalDate.now())
                .build();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private SlaClientReport buildClientReport(List<Ticket> ticketsForClient) {
        Client client = ticketsForClient.get(0).getClient();

        SlaMetric overallResponse = computeMetric(ticketsForClient, true);
        SlaMetric overallResolution = computeMetric(ticketsForClient, false);

        Map<Priority, List<Ticket>> byPriority = ticketsForClient.stream()
                .filter(t -> t.getPriority() != null)
                .collect(Collectors.groupingBy(Ticket::getPriority));

        List<SlaPriorityBreakdown> breakdown = Arrays.stream(Priority.values())
                .map(p -> {
                    List<Ticket> subset = byPriority.getOrDefault(p, Collections.emptyList());
                    return SlaPriorityBreakdown.builder()
                            .priority(p)
                            .totalTickets(subset.size())
                            .response(computeMetric(subset, true))
                            .resolution(computeMetric(subset, false))
                            .build();
                })
                .collect(Collectors.toList());

        return SlaClientReport.builder()
                .clientId(client.getId())
                .clientName(client.getCompanyName())
                .totalTickets(ticketsForClient.size())
                .response(overallResponse)
                .resolution(overallResolution)
                .priorityBreakdown(breakdown)
                .build();
    }

    /**
     * Compute met/missed/pending counts and average hours.
     *
     * @param tickets   subset to evaluate
     * @param isResponse {@code true} = response metric, {@code false} = resolution metric
     */
    private SlaMetric computeMetric(List<Ticket> tickets, boolean isResponse) {
        long met = 0, missed = 0, pending = 0;
        double totalHours = 0;
        long withEvent = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Ticket t : tickets) {
            Priority p = t.getPriority();
            long[] target = (p != null) ? SLA_TARGETS_HOURS.get(p) : null;
            if (target == null) continue;

            long targetHours = isResponse ? target[0] : target[1];
            LocalDateTime eventAt = isResponse ? t.getFirstResponseAt() : t.getResolvedAt();
            LocalDateTime startAt = t.getCreatedAt();

            if (startAt == null) continue;

            if (eventAt != null) {
                double hours = Duration.between(startAt, eventAt).toMinutes() / 60.0;
                totalHours += hours;
                withEvent++;
                if (hours <= targetHours) {
                    met++;
                } else {
                    missed++;
                }
            } else {
                long elapsedHours = Duration.between(startAt, now).toHours();
                if (elapsedHours > targetHours) {
                    missed++; // in-flight breach
                } else {
                    pending++;
                }
            }
        }

        double finished = met + missed;
        double compliance = finished == 0 ? 0.0
                : Math.round((met * 10000.0) / finished) / 100.0;
        double avgHours = withEvent == 0 ? 0.0
                : Math.round((totalHours * 100.0) / withEvent) / 100.0;

        return SlaMetric.builder()
                .met(met)
                .missed(missed)
                .pending(pending)
                .compliancePercent(compliance)
                .averageHours(avgHours)
                .build();
    }

    private SlaReportResponse emptyReport(LocalDate from, LocalDate to) {
        return SlaReportResponse.builder()
                .targets(getSlaTargets())
                .clients(Collections.emptyList())
                .from(from)
                .to(to)
                .generatedAt(LocalDate.now())
                .build();
    }
}
