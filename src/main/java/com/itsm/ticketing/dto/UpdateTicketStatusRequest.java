package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating ticket status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull(message = "Status is required")
    private TicketStatus status;

    @NotNull(message = "Changed by user ID is required")
    private Long changedBy;

    /** Optional notes about the status change */
    private String notes;

    /** Optional summary for when the ticket is resolved or closed */
    private String resolutionSummary;
}
