package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateClientQuotaRequest;
import com.itsm.ticketing.dto.ClientQuotaResponse;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.ClientQuota;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientQuotaRepository;
import com.itsm.ticketing.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing client quotas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientQuotaService {

    private final ClientQuotaRepository clientQuotaRepository;
    private final ClientRepository clientRepository;

    /**
     * Create a new client quota allocation.
     *
     * @param request the quota creation request
     * @return the created quota response
     * @throws ResourceNotFoundException if client is not found
     * @throws IllegalArgumentException  if quota already exists for client+year
     */
    @Transactional
    public ClientQuotaResponse createClientQuota(CreateClientQuotaRequest request) {
        log.info("Creating quota for client ID: {}, year: {}",
                request.getClientId(), request.getYear());

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + request.getClientId()));

        // Check if quota already exists for this client+year
        clientQuotaRepository.findByClientIdAndYear(
                request.getClientId(), request.getYear()
        ).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Quota already exists for client ID: " + request.getClientId()
                            + " and year: " + request.getYear());
        });

        ClientQuota quota = ClientQuota.builder()
                .client(client)
                .year(request.getYear())
                .pmQuota(request.getPmQuota())
                .cmQuota(request.getCmQuota())
                .pmUsed(0)
                .cmUsed(0)
                .build();

        ClientQuota savedQuota = clientQuotaRepository.save(quota);
        log.info("Quota created successfully with ID: {}", savedQuota.getId());

        return mapToResponse(savedQuota);
    }

    /**
     * Get all client quotas.
     *
     * @return list of all quota responses
     */
    @Transactional(readOnly = true)
    public List<ClientQuotaResponse> getAllClientQuotas() {
        return clientQuotaRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a client quota by ID.
     *
     * @param id the quota ID
     * @return the quota response
     * @throws ResourceNotFoundException if quota is not found
     */
    @Transactional(readOnly = true)
    public ClientQuotaResponse getClientQuotaById(Long id) {
        ClientQuota quota = clientQuotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client quota not found with ID: " + id));
        return mapToResponse(quota);
    }

    /**
     * Get quota for a specific client and year.
     *
     * @param clientId the client ID
     * @param year     the year
     * @return the quota response
     * @throws ResourceNotFoundException if quota is not found
     */
    @Transactional(readOnly = true)
    public ClientQuotaResponse getClientQuotaByClientAndYear(Long clientId, Integer year) {
        ClientQuota quota = clientQuotaRepository.findByClientIdAndYear(clientId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Quota not found for client ID: " + clientId
                                + " and year: " + year));
        return mapToResponse(quota);
    }

    /**
     * Update an existing client quota.
     *
     * @param id      the quota ID
     * @param request the update request
     * @return the updated quota response
     * @throws ResourceNotFoundException if quota is not found
     */
    @Transactional
    public ClientQuotaResponse updateClientQuota(Long id, CreateClientQuotaRequest request) {
        log.info("Updating client quota with ID: {}", id);

        ClientQuota quota = clientQuotaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client quota not found with ID: " + id));

        quota.setPmQuota(request.getPmQuota());
        quota.setCmQuota(request.getCmQuota());

        ClientQuota updatedQuota = clientQuotaRepository.save(quota);
        log.info("Client quota updated successfully with ID: {}", updatedQuota.getId());

        return mapToResponse(updatedQuota);
    }

    /**
     * Delete a client quota by ID.
     *
     * @param id the quota ID
     * @throws ResourceNotFoundException if quota is not found
     */
    @Transactional
    public void deleteClientQuota(Long id) {
        log.info("Deleting client quota with ID: {}", id);

        if (!clientQuotaRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Client quota not found with ID: " + id);
        }

        clientQuotaRepository.deleteById(id);
        log.info("Client quota deleted successfully with ID: {}", id);
    }

    /**
     * Maps a ClientQuota entity to a ClientQuotaResponse DTO.
     */
    private ClientQuotaResponse mapToResponse(ClientQuota quota) {
        return ClientQuotaResponse.builder()
                .id(quota.getId())
                .clientId(quota.getClient().getId())
                .clientCompanyName(quota.getClient().getCompanyName())
                .year(quota.getYear())
                .pmQuota(quota.getPmQuota())
                .cmQuota(quota.getCmQuota())
                .pmUsed(quota.getPmUsed())
                .cmUsed(quota.getCmUsed())
                .build();
    }
}
