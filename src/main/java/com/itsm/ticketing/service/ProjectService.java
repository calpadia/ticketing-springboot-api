package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateProjectRequest;
import com.itsm.ticketing.dto.ProjectResponse;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.Project;
import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing projects.
 * Each project belongs to a client. A client can have multiple projects.
 *
 * Access Control:
 * - ADMIN: full access to all projects
 * - USER: read-only access to projects belonging to their client
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;

    /**
     * Create a new project for a client. (ADMIN only - enforced at controller/security level)
     */
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating project '{}' for client ID: {}", request.getProjectName(), request.getClientId());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + request.getClientId()));

        Project project = Project.builder()
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .client(client)
                .isActive(true)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project created with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Get all projects visible to the current user.
     * ADMIN: sees all projects.
     * USER: sees only projects belonging to their client.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects(User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return projectRepository.findAll()
                    .stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        // USER: only see projects from their own client
        if (currentUser.getClient() == null) {
            return Collections.emptyList();
        }

        return projectRepository.findByClientId(currentUser.getClient().getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a project by ID with access control.
     * ADMIN: can see any project.
     * USER: can only see project belonging to their client.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id, User currentUser) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));

        validateProjectAccess(project, currentUser);
        return mapToResponse(project);
    }

    /**
     * Get all projects for a specific client with access control.
     * ADMIN: can query any client.
     * USER: can only query their own client.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByClientId(Long clientId, User currentUser) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with ID: " + clientId);
        }

        // USER can only query their own client's projects
        if (currentUser.getRole() != Role.ADMIN) {
            if (currentUser.getClient() == null ||
                    !currentUser.getClient().getId().equals(clientId)) {
                throw new AccessDeniedException(
                        "Anda hanya dapat melihat project milik client Anda sendiri");
            }
        }

        return projectRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing project. (ADMIN only - enforced at controller/security level)
     */
    @Transactional
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {
        log.info("Updating project with ID: {}", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + request.getClientId()));

        project.setProjectName(request.getProjectName());
        project.setDescription(request.getDescription());
        project.setClient(client);

        Project updated = projectRepository.save(project);
        log.info("Project updated with ID: {}", updated.getId());

        return mapToResponse(updated);
    }

    /**
     * Delete a project by ID. (ADMIN only - enforced at controller/security level)
     */
    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with ID: {}", id);

        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with ID: " + id);
        }

        projectRepository.deleteById(id);
        log.info("Project deleted with ID: {}", id);
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Validates that the current user has access to this project.
     * ADMIN: always allowed.
     * USER: only if the project belongs to their client.
     */
    private void validateProjectAccess(Project project, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getClient() == null ||
                !currentUser.getClient().getId().equals(project.getClient().getId())) {
            throw new AccessDeniedException(
                    "Anda tidak memiliki akses ke project ini");
        }
    }

    /**
     * Maps a Project entity to a ProjectResponse DTO.
     */
    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .clientId(project.getClient().getId())
                .clientCompanyName(project.getClient().getCompanyName())
                .isActive(project.getIsActive())
                .createdAt(project.getCreatedAt())
                .build();
    }
}
