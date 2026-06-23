package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
import com.itsm.ticketing.entity.ProductType;
import com.itsm.ticketing.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for returning ticket information.
 * Avoids exposing full entity relationships and lazy-loading issues.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private MaintenanceType maintenanceType;
    private ProductType productType;

    // Client info (flattened)
    private Long clientId;
    private String clientCompanyName;

    // Project info (flattened, optional)
    private Long projectId;
    private String projectName;

    // Requester info (flattened)
    private Long requesterId;
    private String requesterName;

    // Attachments
    private List<AttachmentResponse> attachments;

    // Assigned support engineers
    private List<TicketAssignmentResponse> assignments;

    private LocalDateTime createdAt;

    /**
     * Whether the currently authenticated user has opened (read) this ticket.
     * <p>
     * {@code false} — user has never opened this ticket detail page → show "NEW" badge.<br>
     * {@code true}  — user has opened this ticket at least once.<br>
     * {@code null}  — not applicable (e.g. response from status-update endpoint).
     * </p>
     */
    private Boolean isRead;
}
