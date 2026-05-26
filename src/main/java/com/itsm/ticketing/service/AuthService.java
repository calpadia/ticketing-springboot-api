package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.AuthResponse;
import com.itsm.ticketing.dto.LoginRequest;
import com.itsm.ticketing.dto.RegisterRequest;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.UserRepository;
import com.itsm.ticketing.security.InputSanitizer;
import com.itsm.ticketing.security.JwtUtils;
import com.itsm.ticketing.security.SecurityAuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for authentication operations: registration and login.
 * Security hardened with:
 * - Password policy enforcement (NIST SP 800-63B)
 * - Input validation and sanitization (OWASP)
 * - Security audit logging (NIST AU-2)
 * - Brute force protection awareness (CWE-307)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final SecurityAuditLogger auditLogger;
    private final HttpServletRequest httpRequest;

    /**
     * Register a new user.
     * USER role requires a clientId to link the user to a client.
     * Enforces password policy per NIST SP 800-63B.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String clientIp = auditLogger.extractIp(httpRequest);

        // Input validation - Password policy (NIST SP 800-63B, CWE-521)
        if (!InputSanitizer.isValidPassword(request.getPassword())) {
            auditLogger.logAuthFailure(request.getEmail(), clientIp, "Weak password");
            throw new IllegalArgumentException(InputSanitizer.getPasswordPolicyMessage());
        }

        // Input validation - Email format
        if (!InputSanitizer.isValidEmail(request.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Input validation - Phone format
        if (!InputSanitizer.isValidPhone(request.getPhone())) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Check for XSS in name field
        if (InputSanitizer.containsXss(request.getName())) {
            auditLogger.logSuspiciousActivity("XSS attempt in registration name",
                    clientIp, "name=" + request.getName());
            throw new IllegalArgumentException("Invalid characters in name field");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            // Don't reveal whether email exists (CWE-204: Observable Response Discrepancy)
            // But for UX, we still return a message - this is a trade-off
            throw new IllegalArgumentException(
                    "Email already registered: " + request.getEmail());
        }

        // Default role to USER if not specified
        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        // Resolve client for USER role
        Client client = null;
        if (role == Role.USER) {
            if (request.getClientId() == null) {
                throw new IllegalArgumentException(
                        "Client ID is required for USER role registration");
            }
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client not found with ID: " + request.getClientId()));
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(role)
                .client(client)
                .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtUtils.generateToken(savedUser);

        // Audit log registration
        auditLogger.logRegistration(savedUser.getEmail(), role.name(), clientIp);

        return AuthResponse.builder()
                .token(jwtToken)
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .clientId(client != null ? client.getId() : null)
                .clientName(client != null ? client.getCompanyName() : null)
                .build();
    }

    /**
     * Authenticate a user with email and password.
     * Returns a JWT token upon successful authentication.
     * Includes audit logging for security monitoring.
     */
    public AuthResponse login(LoginRequest request) {
        String clientIp = auditLogger.extractIp(httpRequest);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            // Audit log failed login attempt (CWE-307 awareness)
            auditLogger.logAuthFailure(request.getEmail(), clientIp, "Invalid credentials");
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String jwtToken = jwtUtils.generateToken(user);

        // Audit log successful login
        auditLogger.logAuthSuccess(user.getEmail(), clientIp);

        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .clientId(user.getClient() != null ? user.getClient().getId() : null)
                .clientName(user.getClient() != null ? user.getClient().getCompanyName() : null)
                .build();
    }
}
