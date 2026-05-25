package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for ticket progress log entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketProgressLogResponse {

    private Long id;
    private Long ticketId;
    private String ticketNumber;
    private TicketStatus fromStatus;
    private TicketStatus toStatus;
    private Long changedById;
    private String changedByName;
    private String notes;
    private LocalDateTime changedAt;
}
