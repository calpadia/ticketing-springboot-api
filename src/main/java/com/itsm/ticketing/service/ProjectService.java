package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateProjectRequest;
import com.itsm.ticketing.dto.ProjectResponse;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.Project;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing projects.
 * Each project belongs to a client. A client can have multiple projects.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;

    /**
     * Create a new project for a client.
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
     * Get all projects.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a project by ID.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with ID: " + id));
        return mapToResponse(project);
    }

    /**
     * Get all projects for a specific client.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByClientId(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with ID: " + clientId);
        }
        return projectRepository.findByClientId(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update an existing project.
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
     * Delete a project by ID.
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
