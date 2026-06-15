package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateServiceCatalogRequest;
import com.itsm.ticketing.dto.ServiceCatalogResponse;
import com.itsm.ticketing.dto.UpdateServiceCatalogRequest;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.ServiceCatalog;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.ServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing service catalog entries.
 * One client = one catalog entry that defines which maintenance services
 * (PM/CM) the client receives, plus optional agreement notes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ClientRepository clientRepository;

    /**
     * Create a new service catalog entry for a client.
     *
     * @throws ResourceNotFoundException if the client does not exist
     * @throws IllegalArgumentException  if the client already has a catalog entry
     */
    @Transactional
    public ServiceCatalogResponse createServiceCatalog(CreateServiceCatalogRequest request) {
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + request.getClientId()));

        if (serviceCatalogRepository.existsByClientId(request.getClientId())) {
            throw new IllegalArgumentException(
                    "Service catalog already exists for client: " + client.getCompanyName()
                            + ". Use update instead.");
        }

        ServiceCatalog catalog = ServiceCatalog.builder()
                .client(client)
                .services(new HashSet<>(request.getServices()))
                .notes(request.getNotes())
                .build();

        ServiceCatalog saved = serviceCatalogRepository.save(catalog);
        log.info("Service catalog created for client {} with services {}",
                client.getCompanyName(), saved.getServices());

        return mapToResponse(saved);
    }

    /**
     * Get all service catalog entries.
     */
    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> getAllServiceCatalogs() {
        return serviceCatalogRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a service catalog by its ID.
     */
    @Transactional(readOnly = true)
    public ServiceCatalogResponse getServiceCatalogById(Long id) {
        ServiceCatalog catalog = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service catalog not found with ID: " + id));
        return mapToResponse(catalog);
    }

    /**
     * Get a service catalog by client ID.
     */
    @Transactional(readOnly = true)
    public ServiceCatalogResponse getServiceCatalogByClientId(Long clientId) {
        ServiceCatalog catalog = serviceCatalogRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service catalog not found for client ID: " + clientId));
        return mapToResponse(catalog);
    }

    /**
     * Update an existing service catalog (services and notes only — client cannot be changed).
     */
    @Transactional
    public ServiceCatalogResponse updateServiceCatalog(Long id, UpdateServiceCatalogRequest request) {
        ServiceCatalog catalog = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service catalog not found with ID: " + id));

        catalog.setServices(new HashSet<>(request.getServices()));
        catalog.setNotes(request.getNotes());

        ServiceCatalog saved = serviceCatalogRepository.save(catalog);
        log.info("Service catalog {} updated for client {}",
                id, catalog.getClient().getCompanyName());

        return mapToResponse(saved);
    }

    /**
     * Delete a service catalog entry.
     */
    @Transactional
    public void deleteServiceCatalog(Long id) {
        ServiceCatalog catalog = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Service catalog not found with ID: " + id));

        serviceCatalogRepository.delete(catalog);
        log.info("Service catalog {} deleted (client {})",
                id, catalog.getClient().getCompanyName());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private ServiceCatalogResponse mapToResponse(ServiceCatalog catalog) {
        return ServiceCatalogResponse.builder()
                .id(catalog.getId())
                .clientId(catalog.getClient().getId())
                .clientCompanyName(catalog.getClient().getCompanyName())
                .services(catalog.getServices())
                .notes(catalog.getNotes())
                .createdAt(catalog.getCreatedAt())
                .updatedAt(catalog.getUpdatedAt())
                .build();
    }
}
