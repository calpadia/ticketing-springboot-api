package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for creating a service catalog entry.
 * Defines which maintenance services (PM/CM) a client receives, plus optional notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServiceCatalogRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    /**
     * Maintenance services offered. Must contain at least one of: PM, CM.
     */
    @NotEmpty(message = "At least one service (PM or CM) must be selected")
    private Set<MaintenanceType> services;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
