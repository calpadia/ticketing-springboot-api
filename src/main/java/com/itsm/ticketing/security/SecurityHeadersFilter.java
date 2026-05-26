package com.itsm.ticketing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security headers filter implementing OWASP Secure Headers recommendations.
 * References:
 * - OWASP Secure Headers Project
 * - NIST SP 800-53 SC-8 (Transmission Confidentiality)
 * - CWE-693 (Protection Mechanism Failure)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Prevent MIME type sniffing (CWE-430)
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking (CWE-1021)
        response.setHeader("X-Frame-Options", "DENY");

        // Enable XSS protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Enforce HTTPS (NIST SC-8)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content Security Policy (CWE-79)
        response.setHeader("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");

        // Prevent information leakage via Referrer
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restrict browser features
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Prevent caching of sensitive responses
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");

        filterChain.doFilter(request, response);
    }
}
