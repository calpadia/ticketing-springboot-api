package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for returning client quota information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientQuotaResponse {

    private Long id;
    private Long clientId;
    private String clientCompanyName;
    private Integer year;
    private Integer pmQuota;
    private Integer cmQuota;
    private Integer pmUsed;
    private Integer cmUsed;
}
