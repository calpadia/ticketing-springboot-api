package com.itsm.ticketing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration following OWASP recommendations.
 * References:
 * - OWASP CORS Misconfiguration
 * - CWE-942 (Overly Permissive Cross-domain Whitelist)
 */
@Configuration
public class CorsConfig {

    @Value("${security.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Only allow specific origins (not wildcard)
        configuration.setAllowedOrigins(
                Arrays.asList(allowedOrigins.split(","))
        );

        // Allowed HTTP methods
        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        // Allowed headers
        configuration.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type",
                        "X-Requested-With",
                        "Accept",
                        "Origin"
                )
        );

        // Expose headers to client
        configuration.setExposedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Disposition"
                )
        );

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/ws/**", configuration);
        return source;
    }
}
