package com.itsm.ticketing.dto;

import com.itsm.ticketing.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for returning user information.
 * Password is excluded for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;

    // Client info (flattened)
    private Long clientId;
    private String clientName;
}
