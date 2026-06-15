package com.itsm.ticketing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a client quota allocation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientQuotaRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Year is required")
    private Integer year;

    @NotNull(message = "PM quota is required")
    @Min(value = 0, message = "PM quota must be >= 0")
    private Integer pmQuota;

    @NotNull(message = "CM quota is required")
    @Min(value = 0, message = "CM quota must be >= 0")
    private Integer cmQuota;
}
