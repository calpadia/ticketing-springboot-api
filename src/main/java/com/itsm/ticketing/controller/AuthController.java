package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.AuthResponse;
import com.itsm.ticketing.dto.LoginRequest;
import com.itsm.ticketing.dto.RegisterRequest;
import com.itsm.ticketing.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication.
 * Provides public endpoints for registration and login.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user.
     *
     * @param request the registration request payload
     * @return the auth response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register - Registering new user");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login with email and password.
     *
     * @param request the login request payload
     * @return the auth response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login - User login attempt");
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
