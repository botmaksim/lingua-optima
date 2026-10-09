/**
 * @file ApiKeyService.java
 * @brief Bring-Your-Own-Key (BYOK) management service for third-party AI provider credentials.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.CreateApiKeyRequest;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @brief Bring-Your-Own-Key (BYOK) management service for third-party AI provider credentials.
 *
 * Implements encrypted storage at rest using AES-256-GCM authenticated encryption.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    /** @brief Field representing api key repository in ApiKeyService. */
    private final ApiKeyRepository apiKeyRepository;
    /** @brief Field representing encryption service in ApiKeyService. */
    private final EncryptionService encryptionService;

    /**
     * @brief Encrypts and securely persists a user's third-party AI API key.
     * @param request Payload containing provider identifier and raw plaintext key.
     * @param user Authenticated user saving the key.
     * @return Saved ApiKey entity with encrypted ciphertext.
     */
    @Transactional
    public ApiKey saveKey(CreateApiKeyRequest request, User user) {
        String encrypted = encryptionService.encrypt(request.getRawKey());

        Optional<ApiKey> existing = apiKeyRepository.findByUserAndProvider(user, request.getProvider());
        ApiKey apiKey;
        if (existing.isPresent()) {
            apiKey = existing.get();
            apiKey.setEncryptedKey(encrypted);
            apiKey.setModelName(request.getModelName());
        } else {
            apiKey = ApiKey.builder()
                .user(user)
                .provider(request.getProvider())
                .modelName(request.getModelName())
                .encryptedKey(encrypted)
                .createdAt(LocalDateTime.now())
                .build();
        }
        return apiKeyRepository.save(apiKey);
    }

    /**
     * @brief Retrieves all encrypted API keys associated with the authenticated user.
     * @param user Authenticated user.
     * @return List of ApiKey entities.
     */
    @Transactional(readOnly = true)
    public List<ApiKey> getKeysForUser(User user) {
        return apiKeyRepository.findAllByUser(user);
    }

    /**
     * @brief Decrypts and retrieves a specific API key for authorized inference execution.
     * @param keyId Unique identifier of the key record.
     * @param user Authenticated user requesting the key.
     * @return Plaintext API key string.
     * @throws ResourceNotFoundException if key is not found.
     * @throws ForbiddenException if key belongs to another user.
     */
    @Transactional(readOnly = true)
    public String getDecryptedKey(UUID keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found: " + keyId));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this API key.");
        }

        return encryptionService.decrypt(apiKey.getEncryptedKey());
    }

    /**
     * @brief Permanently deletes an API key record.
     * @param keyId Unique identifier of the key to delete.
     * @param user Authenticated user requesting deletion.
     * @throws ResourceNotFoundException if key is not found.
     * @throws ForbiddenException if key belongs to another user.
     */
    @Transactional
    public void deleteKey(UUID keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found: " + keyId));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this API key.");
        }

        apiKeyRepository.delete(apiKey);
    }
}
