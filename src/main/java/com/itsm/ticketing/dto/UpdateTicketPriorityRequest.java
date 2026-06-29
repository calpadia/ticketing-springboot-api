package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating the priority (level) of a ticket.
 * Used by SUPPORT and ADMIN roles to escalate or de-escalate ticket priority.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketPriorityRequest {

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Changed by user ID is required")
    private Long changedBy;

    /** Optional notes explaining the reason for the priority change (e.g., "Eskalasi ke L2 karena urgensi client") */
    private String notes;
}
