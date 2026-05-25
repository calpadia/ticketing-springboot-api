package com.itsm.ticketing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for returning client information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientResponse {

    private Long id;
    private String companyName;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private Boolean isActive;
}
