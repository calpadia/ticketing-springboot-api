package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for activating/deactivating a client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateClientStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
