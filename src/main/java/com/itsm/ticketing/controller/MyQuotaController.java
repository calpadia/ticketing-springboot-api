package com.itsm.ticketing.controller;

import com.itsm.ticketing.dto.ClientQuotaResponse;
import com.itsm.ticketing.entity.User;
import com.itsm.ticketing.service.ClientQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * REST controller for authenticated users to access their own client's quotas.
 * Unlike ClientQuotaController (ADMIN-only), this endpoint is accessible by
 * both USER and ADMIN roles. The clientId is derived from the authenticated
 * user's profile — users cannot query quotas for other clients.
 */
@RestController
@RequestMapping("/api/v1/my-quotas")
@RequiredArgsConstructor
@Slf4j
public class MyQuotaController {

    private final ClientQuotaService clientQuotaService;

    /**
     * Get all quotas for the authenticated user's client.
     * Returns an empty list if the user is not associated with any client (e.g., ADMIN).
     *
     * @param user the authenticated user (injected by Spring Security)
     * @return list of quota details for the user's client
     */
    @GetMapping
    public ResponseEntity<List<ClientQuotaResponse>> getMyQuotas(
            @AuthenticationPrincipal User user) {
        log.info("GET /api/v1/my-quotas - User: {} fetching own quotas", user.getEmail());

        if (user.getClient() == null) {
            log.warn("User {} has no associated client, returning empty quotas", user.getEmail());
            return ResponseEntity.ok(Collections.emptyList());
        }

        Long clientId = user.getClient().getId();
        List<ClientQuotaResponse> quotas = clientQuotaService.getQuotasByClientId(clientId);
        return ResponseEntity.ok(quotas);
    }

    /**
     * Get quota for the authenticated user's client for a specific year.
     *
     * @param user the authenticated user (injected by Spring Security)
     * @param year the quota year
     * @return the quota details for the user's client and specified year
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<ClientQuotaResponse> getMyQuotaByYear(
            @AuthenticationPrincipal User user,
            @PathVariable Integer year) {
        log.info("GET /api/v1/my-quotas/year/{} - User: {} fetching own quota", year, user.getEmail());

        if (user.getClient() == null) {
            log.warn("User {} has no associated client", user.getEmail());
            return ResponseEntity.notFound().build();
        }

        Long clientId = user.getClient().getId();
        ClientQuotaResponse response = clientQuotaService.getClientQuotaByClientAndYear(clientId, year);
        return ResponseEntity.ok(response);
    }
}
