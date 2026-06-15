package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateUserRequest;
import com.itsm.ticketing.dto.UserResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing users.
 * Exposes CRUD endpoints for user management.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Create a new user.
     *
     * @param request the user creation request payload
     * @return the created user with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        log.info("POST /api/v1/users - Creating new user");
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all users.
     *
     * @return list of all users
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.info("GET /api/v1/users - Fetching all users");
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get the list of users that the caller can assign as engineer to a ticket.
     * <ul>
     *   <li>ADMIN sees SUPPORT and TECHNICAL_SUPPORT users.</li>
     *   <li>SUPPORT sees TECHNICAL_SUPPORT users only (escalation).</li>
     * </ul>
     * Other roles get 403 from the security layer.
     */
    @GetMapping("/assignable")
    public ResponseEntity<List<UserResponse>> getAssignableEngineers(
            @AuthenticationPrincipal User caller) {
        log.info("GET /api/v1/users/assignable - {} (role: {})",
                caller.getEmail(), caller.getRole());
        return ResponseEntity.ok(userService.getAssignableEngineers(caller));
    }

    /**
     * Get a user by ID.
     *
     * @param id the user ID
     * @return the user details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("GET /api/v1/users/{} - Fetching user by ID", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing user.
     *
     * @param id      the user ID
     * @param request the update request payload
     * @return the updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {
        log.info("PUT /api/v1/users/{} - Updating user", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a user by ID.
     *
     * @param id the user ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/v1/users/{} - Deleting user", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
