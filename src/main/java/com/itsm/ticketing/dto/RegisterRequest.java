package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user registration.
 * Input validation per OWASP Input Validation Cheat Sheet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    /** Optional phone number */
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;

    /**
     * Role for the new user. Defaults to USER if not specified.
     */
    private Role role;

    /**
     * Client ID to link the user to a client.
     * Required for USER role, optional/ignored for ADMIN role.
     */
    private Long clientId;
}
