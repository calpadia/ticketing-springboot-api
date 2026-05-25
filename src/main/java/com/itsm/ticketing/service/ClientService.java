package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.CreateClientRequest;
import com.itsm.ticketing.dto.ClientResponse;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;

    /**
     * Create a new client.
     *
     * @param request the client creation request
     * @return the created client response
     */
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        log.info("Creating client: {}", request.getCompanyName());

        Client client = Client.builder()
                .companyName(request.getCompanyName())
                .contactPersonName(request.getContactPersonName())
                .contactPersonEmail(request.getContactPersonEmail())
                .contactPersonPhone(request.getContactPersonPhone())
                .isActive(true)
                .build();

        Client savedClient = clientRepository.save(client);
        log.info("Client created successfully with ID: {}", savedClient.getId());

        return mapToResponse(savedClient);
    }

    /**
     * Get all clients.
     *
     * @return list of all client responses
     */
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a client by ID.
     *
     * @param id the client ID
     * @return the client response
     * @throws ResourceNotFoundException if client is not found
     */
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + id));
        return mapToResponse(client);
    }

    /**
     * Update an existing client.
     *
     * @param id      the client ID
     * @param request the update request
     * @return the updated client response
     * @throws ResourceNotFoundException if client is not found
     */
    @Transactional
    public ClientResponse updateClient(Long id, CreateClientRequest request) {
        log.info("Updating client with ID: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + id));

        client.setCompanyName(request.getCompanyName());
        client.setContactPersonName(request.getContactPersonName());
        client.setContactPersonEmail(request.getContactPersonEmail());
        client.setContactPersonPhone(request.getContactPersonPhone());

        Client updatedClient = clientRepository.save(client);
        log.info("Client updated successfully with ID: {}", updatedClient.getId());

        return mapToResponse(updatedClient);
    }

    /**
     * Delete a client by ID.
     *
     * @param id the client ID
     * @throws ResourceNotFoundException if client is not found
     */
    @Transactional
    public void deleteClient(Long id) {
        log.info("Deleting client with ID: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client not found with ID: " + id);
        }

        clientRepository.deleteById(id);
        log.info("Client deleted successfully with ID: {}", id);
    }

    /**
     * Maps a Client entity to a ClientResponse DTO.
     */
    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .companyName(client.getCompanyName())
                .contactPersonName(client.getContactPersonName())
                .contactPersonEmail(client.getContactPersonEmail())
                .contactPersonPhone(client.getContactPersonPhone())
                .isActive(client.getIsActive())
                .build();
    }
}
