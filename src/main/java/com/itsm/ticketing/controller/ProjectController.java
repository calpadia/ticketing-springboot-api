package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateProjectRequest;
import com.itsm.ticketing.dto.ProjectResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing projects.
 * Projects belong to clients. A client can have multiple projects.
 *
 * Access Control:
 * - ADMIN: full CRUD on all projects
 * - USER: read-only, hanya project milik client-nya sendiri
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project for a client. (ADMIN only)
     */
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("POST /api/v1/projects - Creating project: {}", request.getProjectName());
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all projects.
     * ADMIN: sees all projects.
     * USER: sees only projects belonging to their client.
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/projects - Fetching projects for user {} (role: {})",
                currentUser.getEmail(), currentUser.getRole());
        return ResponseEntity.ok(projectService.getAllProjects(currentUser));
    }

    /**
     * Get a project by ID.
     * ADMIN: can see any project.
     * USER: can only see project belonging to their client.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/projects/{} - Fetching project", id);
        return ResponseEntity.ok(projectService.getProjectById(id, currentUser));
    }

    /**
     * Get all projects for a specific client.
     * ADMIN: can query any client.
     * USER: can only query their own client.
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClientId(
            @PathVariable Long clientId,
            @AuthenticationPrincipal User currentUser) {
        log.info("GET /api/v1/projects/client/{} - Fetching projects for client", clientId);
        return ResponseEntity.ok(projectService.getProjectsByClientId(clientId, currentUser));
    }

    /**
     * Update a project. (ADMIN only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("PUT /api/v1/projects/{} - Updating project", id);
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /**
     * Delete a project. (ADMIN only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("DELETE /api/v1/projects/{} - Deleting project", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
