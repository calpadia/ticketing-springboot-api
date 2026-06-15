package com.itsm.ticketing.service;

import com.itsm.ticketing.dto.ClientSupportResponse;
import com.itsm.ticketing.dto.ManageClientSupportsRequest;
import com.itsm.ticketing.entity.Client;
import com.itsm.ticketing.entity.ClientSupport;
import com.itsm.ticketing.entity.Role;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.exception.ResourceNotFoundException;
import com.itsm.ticketing.repository.ClientRepository;
import com.itsm.ticketing.repository.ClientSupportRepository;
import com.itsm.ticketing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing the relationship between clients and support engineers.
 * ADMIN assigns support users to clients. When a ticket is created for a client,
 * all support users assigned to that client are auto-assigned to the ticket.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientSupportService {

    private final ClientSupportRepository clientSupportRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    /**
     * Add support engineers to a client.
     * Only users with SUPPORT role can be added.
     *
     * @param clientId the client ID
     * @param request  contains list of support user IDs
     * @return list of created client-support relationships
     */
    @Transactional
    public List<ClientSupportResponse> addSupportsToClient(Long clientId, ManageClientSupportsRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + clientId));

        List<ClientSupportResponse> responses = new ArrayList<>();

        for (Long supportUserId : request.getSupportUserIds()) {
            User supportUser = userRepository.findById(supportUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found with ID: " + supportUserId));

            // Validate user has SUPPORT role
            if (supportUser.getRole() != Role.SUPPORT) {
                throw new IllegalArgumentException(
                        "User " + supportUser.getName() + " (ID: " + supportUserId
                                + ") bukan SUPPORT. Hanya user dengan role SUPPORT yang bisa ditambahkan.");
            }

            // Check if already assigned
            if (clientSupportRepository.existsByClientIdAndSupportUserIdAndActiveTrue(clientId, supportUserId)) {
                log.warn("Support user {} already assigned to client {}, skipping",
                        supportUser.getEmail(), client.getCompanyName());
                continue;
            }

            ClientSupport clientSupport = ClientSupport.builder()
                    .client(client)
                    .supportUser(supportUser)
                    .active(true)
                    .build();

            ClientSupport saved = clientSupportRepository.save(clientSupport);
            responses.add(mapToResponse(saved));

            log.info("Support {} added to client {}",
                    supportUser.getName(), client.getCompanyName());
        }

        return responses;
    }

    /**
     * Remove support engineers from a client.
     *
     * @param clientId the client ID
     * @param request  contains list of support user IDs to remove
     */
    @Transactional
    public void removeSupportsFromClient(Long clientId, ManageClientSupportsRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Client not found with ID: " + clientId));

        for (Long supportUserId : request.getSupportUserIds()) {
            ClientSupport clientSupport = clientSupportRepository
                    .findByClientIdAndSupportUserIdAndActiveTrue(clientId, supportUserId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Support user ID " + supportUserId
                                    + " tidak terdaftar di client " + client.getCompanyName()));

            clientSupport.setActive(false);
            clientSupportRepository.save(clientSupport);

            log.info("Support user ID {} removed from client {}",
                    supportUserId, client.getCompanyName());
        }
    }

    /**
     * Get all active support engineers for a client.
     *
     * @param clientId the client ID
     * @return list of active support assignments
     */
    @Transactional(readOnly = true)
    public List<ClientSupportResponse> getSupportsByClient(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found with ID: " + clientId);
        }

        return clientSupportRepository.findByClientIdAndActiveTrue(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active support user entities for a client (used internally for auto-assign).
     *
     * @param clientId the client ID
     * @return list of support User entities
     */
    @Transactional(readOnly = true)
    public List<User> getActiveSupportUsersForClient(Long clientId) {
        return clientSupportRepository.findByClientIdAndActiveTrue(clientId)
                .stream()
                .map(ClientSupport::getSupportUser)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private ClientSupportResponse mapToResponse(ClientSupport cs) {
        return ClientSupportResponse.builder()
                .id(cs.getId())
                .clientId(cs.getClient().getId())
                .clientCompanyName(cs.getClient().getCompanyName())
                .supportUserId(cs.getSupportUser().getId())
                .supportUserName(cs.getSupportUser().getName())
                .supportUserEmail(cs.getSupportUser().getEmail())
                .assignedAt(cs.getAssignedAt())
                .active(cs.isActive())
                .build();
    }
}
