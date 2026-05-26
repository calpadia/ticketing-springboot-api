package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Request DTO for updating a service catalog entry.
 * Client cannot be changed once created — only services and notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServiceCatalogRequest {

    @NotEmpty(message = "At least one service (PM or CM) must be selected")
    private Set<MaintenanceType> services;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
