package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for ticket assignment information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketAssignmentResponse {

    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private String ticketTitle;

    // Assigned support engineer info
    private Long assignedToId;
    private String assignedToName;
    private String assignedToEmail;

    // Who assigned
    private Long assignedById;
    private String assignedByName;

    private String notes;
    private LocalDateTime assignedAt;
    private boolean active;
}
