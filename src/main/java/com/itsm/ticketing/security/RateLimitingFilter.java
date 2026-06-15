package com.itsm.ticketing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itsm.ticketing.dto.ApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent brute force attacks on authentication endpoints.
 * Implements OWASP recommendations for credential stuffing protection.
 * References:
 * - OWASP Testing Guide: Brute Force (OTG-AUTHN-003)
 * - CWE-307 (Improper Restriction of Excessive Authentication Attempts)
 * - NIST SP 800-63B Section 5.2.2 (Rate Limiting)
 * - BSSN: Standar Keamanan Aplikasi - Pembatasan Akses
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // Rate limit buckets per IP address
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    /**
     * Auth endpoints: 10 requests per minute per IP (stricter for login/register).
     */
    private Bucket createAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .build();
    }

    /**
     * General API endpoints: 100 requests per minute per IP.
     */
    private Bucket createApiBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .build();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        Bucket bucket;
        if (requestUri.startsWith("/api/v1/auth/")) {
            // Stricter rate limit for auth endpoints
            bucket = authBuckets.computeIfAbsent(clientIp, k -> createAuthBucket());
        } else if (requestUri.startsWith("/api/")) {
            // General rate limit for API endpoints
            bucket = apiBuckets.computeIfAbsent(clientIp, k -> createApiBucket());
        } else {
            // No rate limiting for non-API endpoints (WebSocket, etc.)
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("SECURITY_AUDIT: Rate limit exceeded for IP: {} on endpoint: {}",
                    clientIp, requestUri);

            ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .message("Rate limit exceeded. Please try again later.")
                    .timestamp(LocalDateTime.now())
                    .build();

            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(response.getOutputStream(), errorResponse);
        }
    }

    /**
     * Extract client IP, considering proxy headers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
