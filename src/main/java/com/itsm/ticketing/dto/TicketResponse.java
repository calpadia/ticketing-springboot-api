package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
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

    // Client info (flattened)
    private Long clientId;
    private String clientCompanyName;

    // Requester info (flattened)
    private Long requesterId;
    private String requesterName;

    // Attachments
    private List<AttachmentResponse> attachments;

    private LocalDateTime createdAt;
}
