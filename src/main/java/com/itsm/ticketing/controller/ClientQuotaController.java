package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.CreateClientQuotaRequest;
import com.itsm.ticketing.dto.ClientQuotaResponse;
import com.itsm.ticketing.service.ClientQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing client quotas.
 * Exposes CRUD endpoints for client quota management.
 */
@RestController
@RequestMapping("/api/v1/client-quotas")
@RequiredArgsConstructor
@Slf4j
public class ClientQuotaController {

    private final ClientQuotaService clientQuotaService;

    /**
     * Create a new client quota.
     *
     * @param request the quota creation request payload
     * @return the created quota with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ClientQuotaResponse> createClientQuota(
            @Valid @RequestBody CreateClientQuotaRequest request) {
        log.info("POST /api/v1/client-quotas - Creating new client quota");
        ClientQuotaResponse response = clientQuotaService.createClientQuota(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all client quotas.
     *
     * @return list of all client quotas
     */
    @GetMapping
    public ResponseEntity<List<ClientQuotaResponse>> getAllClientQuotas() {
        log.info("GET /api/v1/client-quotas - Fetching all client quotas");
        List<ClientQuotaResponse> quotas = clientQuotaService.getAllClientQuotas();
        return ResponseEntity.ok(quotas);
    }

    /**
     * Get a client quota by ID.
     *
     * @param id the quota ID
     * @return the quota details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientQuotaResponse> getClientQuotaById(@PathVariable Long id) {
        log.info("GET /api/v1/client-quotas/{} - Fetching quota by ID", id);
        ClientQuotaResponse response = clientQuotaService.getClientQuotaById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get quota for a specific client and year.
     *
     * @param clientId the client ID
     * @param year     the year
     * @return the quota details
     */
    @GetMapping("/client/{clientId}/year/{year}")
    public ResponseEntity<ClientQuotaResponse> getClientQuotaByClientAndYear(
            @PathVariable Long clientId,
            @PathVariable Integer year) {
        log.info("GET /api/v1/client-quotas/client/{}/year/{} - Fetching quota", clientId, year);
        ClientQuotaResponse response = clientQuotaService.getClientQuotaByClientAndYear(clientId, year);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing client quota.
     *
     * @param id      the quota ID
     * @param request the update request payload
     * @return the updated quota
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientQuotaResponse> updateClientQuota(
            @PathVariable Long id,
            @Valid @RequestBody CreateClientQuotaRequest request) {
        log.info("PUT /api/v1/client-quotas/{} - Updating quota", id);
        ClientQuotaResponse response = clientQuotaService.updateClientQuota(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a client quota by ID.
     *
     * @param id the quota ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientQuota(@PathVariable Long id) {
        log.info("DELETE /api/v1/client-quotas/{} - Deleting quota", id);
        clientQuotaService.deleteClientQuota(id);
        return ResponseEntity.noContent().build();
    }
}
