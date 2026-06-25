package com.itsm.ticketing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.itsm.ticketing.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Handles access-denied (403 Forbidden) errors for authenticated users
 * who lack the required role or permission.
 *
 * <p>Without this handler, Spring Security forwards the 403 to the internal
 * {@code /error} route which itself requires authentication, causing the
 * response to be replaced with a 401 — tricking the frontend into treating
 * a permission error as an expired session and logging the user out.</p>
 *
 * <p>This handler short-circuits that forwarding by writing the 403 JSON
 * response directly to the {@link HttpServletResponse} output stream.</p>
 */
@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Access denied for [{}] {}: {}",
                request.getMethod(),
                request.getRequestURI(),
                accessDeniedException.getMessage());

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message("Anda tidak memiliki izin (role) untuk mengakses resource ini.")
                .timestamp(LocalDateTime.now())
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
