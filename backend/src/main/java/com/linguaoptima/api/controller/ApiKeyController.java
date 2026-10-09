package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.CreateApiKeyRequest;
import com.linguaoptima.api.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @file ApiKeyController.java
 * @brief REST controller for managing custom AI provider API keys (BYOK).
 *
 * Provides endpoints to list configured keys, securely persist encrypted keys,
 * and revoke existing custom provider integrations.
 */
@RestController
@RequestMapping({"/api/apikeys", "/api/api-keys"})
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * @brief Retrieves all configured API keys for the authenticated user.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with list of ApiKey objects containing masked key values.
     */
    @GetMapping
    public ResponseEntity<List<ApiKey>> getApiKeys(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(apiKeyService.getKeysForUser(user));
    }

    /**
     * @brief Persists an encrypted API key for an AI provider.
     *
     * @param user Authenticated user principal.
     * @param request Payload containing AI provider name and plaintext API key.
     * @return HTTP 200 on successful encryption and persistence.
     */
    @PostMapping
    public ResponseEntity<Void> addApiKey(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        apiKeyService.saveKey(request, user);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Deletes a previously saved AI API key.
     *
     * @param id Unique identifier of the key to revoke.
     * @param user Authenticated user principal.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal User user
    ) {
        apiKeyService.deleteKey(id, user);
        return ResponseEntity.noContent().build();
    }
}
