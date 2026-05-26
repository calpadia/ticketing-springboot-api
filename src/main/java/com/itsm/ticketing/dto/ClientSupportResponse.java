package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for client-support relationship.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientSupportResponse {

    private Long id;
    private Long clientId;
    private String clientCompanyName;
    private Long supportUserId;
    private String supportUserName;
    private String supportUserEmail;
    private LocalDateTime assignedAt;
    private boolean active;
}
