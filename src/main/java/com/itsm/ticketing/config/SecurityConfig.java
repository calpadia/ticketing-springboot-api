package com.itsm.ticketing.config;

import com.itsm.ticketing.security.JwtAuthenticationEntryPoint;
import com.itsm.ticketing.security.JwtAuthenticationFilter;
import com.itsm.ticketing.security.RateLimitingFilter;
import com.itsm.ticketing.security.SecurityHeadersFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration.
 * Configures JWT-based stateless authentication, role-based access control,
 * CORS, security headers, and rate limiting.
 *
 * Security Standards:
 * - OWASP Top 10 (A01-A10)
 * - NIST SP 800-53 (AC, AU, SC controls)
 * - CWE/SANS Top 25
 * - BSSN Standar Keamanan Aplikasi
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final JwtAuthenticationEntryPoint jwtAuthEntryPoint;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CorsConfigurationSource corsConfigurationSource;
    private final RateLimitingFilter rateLimitingFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (stateless REST API with JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Enable CORS with strict configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // Exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthEntryPoint)
                )

                // Stateless session management (NIST IA-11)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Endpoint authorization rules (NIST AC-3, OWASP A01)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (auth)
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // WebSocket handshake endpoint
                        .requestMatchers("/ws/**").permitAll()

                        // Actuator health endpoint (for monitoring)
                        .requestMatchers("/actuator/health").permitAll()

                        // Admin-only endpoints
                        // NOTE: /users/assignable is exposed to SUPPORT for ticket-assignment dropdowns,
                        // so it must be matched BEFORE the broader /users/** ADMIN rule.
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/assignable").hasAnyRole("ADMIN", "SUPPORT")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/clients/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/client-quotas/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/service-catalogs/**").hasRole("ADMIN")

                        // Project endpoints - ADMIN full CRUD, USER can read own client's projects
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/projects/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/**").hasAnyRole("ADMIN", "USER")

                        // User's own quota endpoint - accessible by ADMIN and USER
                        .requestMatchers(HttpMethod.GET, "/api/v1/my-quotas/**").hasAnyRole("ADMIN", "USER")

                        // SLA report - ADMIN sees all, USER scoped to own client (server-enforced)
                        .requestMatchers(HttpMethod.GET, "/api/v1/sla-report/**").hasAnyRole("ADMIN", "USER")

                        // Ticket endpoints - accessible by ADMIN, SUPPORT, TECHNICAL_SUPPORT, and USER
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/my-assignments").hasAnyRole("ADMIN", "SUPPORT", "TECHNICAL_SUPPORT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/tickets/**").hasAnyRole("ADMIN", "SUPPORT", "TECHNICAL_SUPPORT", "USER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/tickets/**").hasAnyRole("ADMIN", "SUPPORT", "TECHNICAL_SUPPORT", "USER")
                        // SUPPORT boleh assign/reassign ticket ke TECHNICAL_SUPPORT (eskalasi)
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/*/assign").hasAnyRole("ADMIN", "SUPPORT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/*/unassign").hasAnyRole("ADMIN", "SUPPORT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/tickets/*/reassign").hasAnyRole("ADMIN", "SUPPORT")

                        // Chat endpoints - accessible by ADMIN, SUPPORT, TECHNICAL_SUPPORT, and USER
                        .requestMatchers("/api/v1/chat/**").hasAnyRole("ADMIN", "SUPPORT", "TECHNICAL_SUPPORT", "USER")

                        // Attachment endpoints - accessible by ADMIN, SUPPORT, TECHNICAL_SUPPORT, and USER
                        .requestMatchers("/api/v1/attachments/**").hasAnyRole("ADMIN", "SUPPORT", "TECHNICAL_SUPPORT", "USER")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add security filters in correct order
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityHeadersFilter, RateLimitingFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
