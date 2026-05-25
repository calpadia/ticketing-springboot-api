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
import com.itsm.ticketing.security.JwtUtils;
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

    /**
     * Register a new user.
     * USER role requires a clientId to link the user to a client.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
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
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .client(client)
                .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtUtils.generateToken(savedUser);

        log.info("User registered successfully: {} (role: {}, client: {})",
                savedUser.getEmail(), role, client != null ? client.getCompanyName() : "N/A");

        return AuthResponse.builder()
                .token(jwtToken)
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    /**
     * Authenticate a user with email and password.
     * Returns a JWT token upon successful authentication.
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String jwtToken = jwtUtils.generateToken(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
