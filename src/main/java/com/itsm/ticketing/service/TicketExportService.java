package com.itsm.ticketing.service;

import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.Ticket;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.repository.TicketAssignmentRepository;
import com.itsm.ticketing.repository.TicketRepository;
import com.itsm.ticketing.repository.TicketSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for exporting tickets to CSV.
 *
 * <p>Filters: client, date range (from/to). All optional.</p>
 *
 * <p>Access control is enforced server-side regardless of what the caller
 * passes:</p>
 * <ul>
 *     <li>ADMIN — can export anything; all filters honoured.</li>
 *     <li>USER — restricted to their own client (incoming clientId is
 *         overridden to the user's client; if user has no client, returns
 *         empty CSV).</li>
 *     <li>SUPPORT / TECHNICAL_SUPPORT — restricted to tickets assigned to
 *         them (the optional clientId filter is still applied on top).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TicketExportService {

    private static final String CSV_HEADER =
            "Ticket Number,Title,Description,Status,Priority,Maintenance Type,Product Type," +
                    "Client,Project,Requester,Created At";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketRepository ticketRepository;
    private final TicketAssignmentRepository assignmentRepository;

    /**
     * Stream filtered tickets directly to the given OutputStream as CSV.
     * Streaming avoids loading the entire CSV into memory.
     *
     * @param caller   the authenticated user (used for access control)
     * @param clientId optional client filter (ignored or overridden per role)
     * @param from     optional start date (inclusive). null = no lower bound.
     * @param to       optional end date (inclusive). null = no upper bound.
     * @param out      output stream to write to (e.g. HTTP response body)
     * @throws IllegalArgumentException if {@code from} is after {@code to}
     * @throws IOException              if writing fails
     */
    @Transactional(readOnly = true)
    public void exportTicketsAsCsv(
            User caller,
            Long clientId,
            LocalDate from,
            LocalDate to,
            OutputStream out) throws IOException {

        // 1. Validate input
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' tidak boleh setelah 'to'");
        }

        // 2. Resolve filters based on caller role (server-side enforcement)
        Long effectiveClientId = clientId;
        List<Long> ticketIdScope = null;     // null = no whitelist filter

        switch (caller.getRole()) {
            case ADMIN -> {
                // No additional restriction
            }
            case USER -> {
                if (caller.getClient() == null) {
                    log.info("USER {} has no client, returning empty CSV", caller.getEmail());
                    writeEmptyCsv(out);
                    return;
                }
                // Force clientId to the user's own client
                effectiveClientId = caller.getClient().getId();
            }
            case SUPPORT, TECHNICAL_SUPPORT -> {
                // Only tickets assigned to this user
                ticketIdScope = assignmentRepository.findByAssignedToIdAndActiveTrue(caller.getId())
                        .stream()
                        .map(a -> a.getTicket().getId())
                        .collect(Collectors.toList());
                if (ticketIdScope.isEmpty()) {
                    log.info("{} {} has no active assignments, returning empty CSV",
                            caller.getRole(), caller.getEmail());
                    writeEmptyCsv(out);
                    return;
                }
            }
            default -> throw new AccessDeniedException(
                    "Role " + caller.getRole() + " tidak dapat melakukan export");
        }

        // 3. Build datetime bounds (inclusive)
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt = to != null ? to.atTime(LocalTime.MAX) : null;

        // 4. Query (Specifications skip null filters at SQL level)
        Specification<Ticket> spec = TicketSpecifications.allOf(
                TicketSpecifications.withClientId(effectiveClientId),
                TicketSpecifications.withIdIn(ticketIdScope),
                TicketSpecifications.createdAtFrom(fromDt),
                TicketSpecifications.createdAtTo(toDt)
        );
        List<Ticket> tickets = (spec == null)
                ? ticketRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : ticketRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        log.info("Exporting {} ticket(s) for {} (clientId={}, from={}, to={})",
                tickets.size(), caller.getEmail(), effectiveClientId, from, to);

        // 5. Write CSV (UTF-8 BOM so Excel auto-detects encoding)
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.write("\r\n");
            for (Ticket t : tickets) {
                writer.write(toCsvRow(t));
                writer.write("\r\n");
            }
            writer.flush();
        }
    }

    /**
     * Build a default filename for the download, e.g. tickets-2026-01-01_to_2026-05-31.csv.
     */
    public String buildFilename(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder("tickets");
        if (from != null) sb.append('-').append(from);
        if (to != null) sb.append("_to_").append(to);
        if (from == null && to == null) {
            sb.append('-').append(LocalDate.now());
        }
        sb.append(".csv");
        return sb.toString();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private void writeEmptyCsv(OutputStream out) throws IOException {
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.write("\r\n");
            writer.flush();
        }
    }

    private String toCsvRow(Ticket t) {
        return String.join(",",
                csv(t.getTicketNumber()),
                csv(t.getTitle()),
                csv(t.getDescription()),
                csv(t.getStatus() != null ? t.getStatus().name() : null),
                csv(t.getPriority() != null ? t.getPriority().name() : null),
                csv(t.getMaintenanceType() != null ? t.getMaintenanceType().name() : null),
                csv(t.getProductType() != null ? t.getProductType().name() : null),
                csv(t.getClient() != null ? t.getClient().getCompanyName() : null),
                csv(t.getProject() != null ? t.getProject().getProjectName() : null),
                csv(t.getRequester() != null ? t.getRequester().getName() : null),
                csv(t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_FMT) : null)
        );
    }

    /**
     * Escape a value for CSV per RFC 4180.
     * Wraps in double quotes if the value contains comma, quote, CR, or LF.
     * Embedded double quotes are doubled.
     */
    private String csv(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
