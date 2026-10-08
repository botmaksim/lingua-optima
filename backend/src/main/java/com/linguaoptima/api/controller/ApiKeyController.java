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

@RestController
@RequestMapping({"/api/apikeys", "/api/api-keys"})
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public ResponseEntity<List<ApiKey>> getApiKeys(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(apiKeyService.getKeysForUser(user));
    }

    @PostMapping
    public ResponseEntity<Void> addApiKey(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateApiKeyRequest request
    ) {
        apiKeyService.saveKey(request, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiKey(
        @PathVariable("id") UUID id,
        @AuthenticationPrincipal User user
    ) {
        apiKeyService.deleteKey(id, user);
        return ResponseEntity.noContent().build();
    }
}
