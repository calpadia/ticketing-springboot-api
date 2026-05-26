package com.itsm.ticketing.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding/removing support engineers to/from a client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManageClientSupportsRequest {

    /**
     * List of user IDs (must have SUPPORT role) to add/remove.
     */
    @NotEmpty(message = "At least one support user ID is required")
    private List<Long> supportUserIds;
}
