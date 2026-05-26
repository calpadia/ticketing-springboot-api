package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateServiceCatalogRequest;
import com.itsm.ticketing.dto.ServiceCatalogResponse;
import com.itsm.ticketing.dto.UpdateServiceCatalogRequest;
import com.itsm.ticketing.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing service catalog entries.
 * One client may have at most one catalog entry that defines which
 * maintenance services (PM/CM) the client receives.
 *
 * Access control: ADMIN only (managed in SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/service-catalogs")
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    /**
     * Create a new service catalog entry for a client.
     */
    @PostMapping
    public ResponseEntity<ServiceCatalogResponse> createServiceCatalog(
            @Valid @RequestBody CreateServiceCatalogRequest request) {
        log.info("POST /api/v1/service-catalogs - Creating catalog for client {}",
                request.getClientId());
        ServiceCatalogResponse response = serviceCatalogService.createServiceCatalog(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all service catalog entries.
     */
    @GetMapping
    public ResponseEntity<List<ServiceCatalogResponse>> getAllServiceCatalogs() {
        log.info("GET /api/v1/service-catalogs - Fetching all catalogs");
        return ResponseEntity.ok(serviceCatalogService.getAllServiceCatalogs());
    }

    /**
     * Get a service catalog by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> getServiceCatalogById(@PathVariable Long id) {
        log.info("GET /api/v1/service-catalogs/{} - Fetching catalog", id);
        return ResponseEntity.ok(serviceCatalogService.getServiceCatalogById(id));
    }

    /**
     * Get a service catalog by client ID.
     */
    @GetMapping("/client/{clientId}")
    public ResponseEntity<ServiceCatalogResponse> getServiceCatalogByClientId(
            @PathVariable Long clientId) {
        log.info("GET /api/v1/service-catalogs/client/{} - Fetching catalog by client", clientId);
        return ResponseEntity.ok(serviceCatalogService.getServiceCatalogByClientId(clientId));
    }

    /**
     * Update an existing service catalog (services & notes only).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceCatalogResponse> updateServiceCatalog(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceCatalogRequest request) {
        log.info("PUT /api/v1/service-catalogs/{} - Updating catalog", id);
        return ResponseEntity.ok(serviceCatalogService.updateServiceCatalog(id, request));
    }

    /**
     * Delete a service catalog entry.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCatalog(@PathVariable Long id) {
        log.info("DELETE /api/v1/service-catalogs/{} - Deleting catalog", id);
        serviceCatalogService.deleteServiceCatalog(id);
        return ResponseEntity.noContent().build();
    }
}
