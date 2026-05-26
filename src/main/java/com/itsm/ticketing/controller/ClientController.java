package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.ClientSupportResponse;
import com.itsm.ticketing.dto.CreateClientRequest;
import com.itsm.ticketing.dto.ClientResponse;
import com.itsm.ticketing.dto.ManageClientSupportsRequest;
import com.itsm.ticketing.service.ClientService;
import com.itsm.ticketing.service.ClientSupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing clients.
 * Exposes CRUD endpoints for client management and support assignment.
 */
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;
    private final ClientSupportService clientSupportService;

    /**
     * Create a new client.
     *
     * @param request the client creation request payload
     * @return the created client with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @Valid @RequestBody CreateClientRequest request) {
        log.info("POST /api/v1/clients - Creating new client");
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all clients.
     *
     * @return list of all clients
     */
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        log.info("GET /api/v1/clients - Fetching all clients");
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    /**
     * Get a client by ID.
     *
     * @param id the client ID
     * @return the client details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Long id) {
        log.info("GET /api/v1/clients/{} - Fetching client by ID", id);
        ClientResponse response = clientService.getClientById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing client.
     *
     * @param id      the client ID
     * @param request the update request payload
     * @return the updated client
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody CreateClientRequest request) {
        log.info("PUT /api/v1/clients/{} - Updating client", id);
        ClientResponse response = clientService.updateClient(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a client by ID.
     *
     * @param id the client ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        log.info("DELETE /api/v1/clients/{} - Deleting client", id);
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }

    // ========================================================================
    // CLIENT SUPPORT MANAGEMENT
    // ========================================================================

    /**
     * Add support engineers to a client.
     * These support users will be auto-assigned to new tickets from this client.
     *
     * @param clientId the client ID
     * @param request  contains list of support user IDs
     * @return list of created assignments
     */
    @PostMapping("/{clientId}/supports")
    public ResponseEntity<List<ClientSupportResponse>> addSupports(
            @PathVariable Long clientId,
            @Valid @RequestBody ManageClientSupportsRequest request) {
        log.info("POST /api/v1/clients/{}/supports - Adding {} support(s)",
                clientId, request.getSupportUserIds().size());
        List<ClientSupportResponse> responses =
                clientSupportService.addSupportsToClient(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Remove support engineers from a client.
     *
     * @param clientId the client ID
     * @param request  contains list of support user IDs to remove
     */
    @DeleteMapping("/{clientId}/supports")
    public ResponseEntity<Void> removeSupports(
            @PathVariable Long clientId,
            @Valid @RequestBody ManageClientSupportsRequest request) {
        log.info("DELETE /api/v1/clients/{}/supports - Removing {} support(s)",
                clientId, request.getSupportUserIds().size());
        clientSupportService.removeSupportsFromClient(clientId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all active support engineers for a client.
     *
     * @param clientId the client ID
     * @return list of support assignments
     */
    @GetMapping("/{clientId}/supports")
    public ResponseEntity<List<ClientSupportResponse>> getClientSupports(
            @PathVariable Long clientId) {
        log.info("GET /api/v1/clients/{}/supports - Fetching supports", clientId);
        List<ClientSupportResponse> supports =
                clientSupportService.getSupportsByClient(clientId);
        return ResponseEntity.ok(supports);
    }
}
