package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateProjectRequest;
import com.itsm.ticketing.dto.ProjectResponse;
import com.itsm.ticketing.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing projects.
 * Projects belong to clients. A client can have multiple projects.
 * All endpoints are ADMIN-only.
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Create a new project for a client.
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
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        log.info("GET /api/v1/projects - Fetching all projects");
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    /**
     * Get a project by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        log.info("GET /api/v1/projects/{} - Fetching project", id);
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    /**
     * Get all projects for a specific client.
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByClientId(
            @PathVariable Long clientId) {
        log.info("GET /api/v1/projects/client/{} - Fetching projects for client", clientId);
        return ResponseEntity.ok(projectService.getProjectsByClientId(clientId));
    }

    /**
     * Update a project.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("PUT /api/v1/projects/{} - Updating project", id);
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /**
     * Delete a project.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("DELETE /api/v1/projects/{} - Deleting project", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
