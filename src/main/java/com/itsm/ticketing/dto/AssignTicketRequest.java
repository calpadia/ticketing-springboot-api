package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for assigning support engineers to a ticket.
 * Supports assigning multiple support users at once.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignTicketRequest {

    /**
     * List of user IDs (SUPPORT role) to assign to the ticket.
     */
    @NotEmpty(message = "At least one support user ID is required")
    private List<Long> supportUserIds;

    /**
     * Optional notes about the assignment.
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
