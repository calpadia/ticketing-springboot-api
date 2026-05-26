package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for unassigning support engineers from a ticket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnassignTicketRequest {

    /**
     * List of user IDs to unassign from the ticket.
     */
    @NotEmpty(message = "At least one support user ID is required")
    private List<Long> supportUserIds;

    /**
     * Optional reason for unassignment.
     */
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
}
