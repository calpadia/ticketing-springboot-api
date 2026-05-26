package com.itsm.ticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separate config for PasswordEncoder to avoid circular dependency
 * between SecurityConfig → JwtAuthenticationFilter → UserService → PasswordEncoder.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 (OWASP recommendation, NIST SP 800-63B)
        // Higher strength = more computational cost for brute force attacks
        return new BCryptPasswordEncoder(12);
    }
}
