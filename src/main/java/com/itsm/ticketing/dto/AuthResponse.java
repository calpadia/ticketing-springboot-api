package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for authentication (login/register).
 * Contains JWT token and user details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private Long clientId;
    private String clientName;
}
