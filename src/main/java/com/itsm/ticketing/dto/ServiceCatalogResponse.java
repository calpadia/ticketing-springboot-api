package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.MaintenanceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response DTO for service catalog entries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCatalogResponse {

    private Long id;
    private Long clientId;
    private String clientCompanyName;
    private Set<MaintenanceType> services;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
