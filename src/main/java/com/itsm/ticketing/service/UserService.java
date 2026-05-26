package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateUserRequest;
import com.itsm.ticketing.dto.UserResponse;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing users.
 * Also implements UserDetailsService for Spring Security authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    // ========================================================================
    // UserDetailsService implementation (for Spring Security)
    // ========================================================================

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));
    }

    // ========================================================================
    // CRUD Operations
    // ========================================================================

    /**
     * Create a new user. Password is hashed with BCrypt.
     *
     * @param request the user creation request
     * @return the created user response
     * @throws IllegalArgumentException if email already exists
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.getEmail());
        }

        // Resolve client if provided
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client not found with ID: " + request.getClientId()));
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .client(client)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        return mapToResponse(savedUser);
    }

    /**
     * Get all users.
     *
     * @return list of all user responses
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a user by ID.
     *
     * @param id the user ID
     * @return the user response
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + id));
        return mapToResponse(user);
    }

    /**
     * Update an existing user. Password is re-hashed with BCrypt.
     *
     * @param id      the user ID
     * @param request the update request
     * @return the updated user response
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional
    public UserResponse updateUser(Long id, CreateUserRequest request) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + id));

        // Check if email is being changed and already exists
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.getEmail());
        }

        // Resolve client if provided
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client not found with ID: " + request.getClientId()));
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());
        user.setClient(client);

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", updatedUser.getId());

        return mapToResponse(updatedUser);
    }

    /**
     * Delete a user by ID.
     *
     * @param id the user ID
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }

    /**
     * Get users that the caller is allowed to assign as engineer for a ticket.
     * <ul>
     *   <li>ADMIN: SUPPORT + TECHNICAL_SUPPORT engineers</li>
     *   <li>SUPPORT: TECHNICAL_SUPPORT engineers only (escalation flow)</li>
     *   <li>Other roles: not allowed (handled at controller/security layer)</li>
     * </ul>
     *
     * @param caller the authenticated user requesting the list
     * @return list of users eligible to be assigned by the caller
     * @throws org.springframework.security.access.AccessDeniedException if caller has no
     *                                                                   permission to assign
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAssignableEngineers(User caller) {
        List<Role> targetRoles;
        switch (caller.getRole()) {
            case ADMIN -> targetRoles = List.of(Role.SUPPORT, Role.TECHNICAL_SUPPORT);
            case SUPPORT -> targetRoles = List.of(Role.TECHNICAL_SUPPORT);
            default -> throw new org.springframework.security.access.AccessDeniedException(
                    "Role " + caller.getRole() + " tidak dapat melihat daftar assignable engineer");
        }

        return userRepository.findByRoleIn(targetRoles)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a User entity to a UserResponse DTO.
     */
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
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
