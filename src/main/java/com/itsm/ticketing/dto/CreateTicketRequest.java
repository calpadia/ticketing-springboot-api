package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import com.itsm.ticketing.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new ticket.
 * Input validation per OWASP Input Validation Cheat Sheet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 5000, message = "Description must be between 10 and 5000 characters")
    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    @NotNull(message = "Client ID is required")
    private Long clientId;

    /**
     * Optional project ID. If provided, ticket will be linked to this project.
     */
    private Long projectId;

    @NotNull(message = "Requester ID is required")
    private Long requesterId;
}
