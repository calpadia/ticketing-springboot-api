package com.itsm.ticketing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new client.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClientRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Contact person email is required")
    @Email(message = "Contact person email must be valid")
    private String contactPersonEmail;

    @NotBlank(message = "Contact person phone is required")
    private String contactPersonPhone;
}
