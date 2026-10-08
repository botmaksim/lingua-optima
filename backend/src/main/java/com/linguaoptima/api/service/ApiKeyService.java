package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public ApiKey saveKey(CreateApiKeyRequest request, User user) {
        String encrypted = encryptionService.encrypt(request.getRawKey());

        Optional<ApiKey> existing = apiKeyRepository.findByUserAndProvider(user, request.getProvider());
        ApiKey apiKey;
        if (existing.isPresent()) {
            apiKey = existing.get();
            apiKey.setEncryptedKey(encrypted);
        } else {
            apiKey = ApiKey.builder()
                .user(user)
                .provider(request.getProvider())
                .encryptedKey(encrypted)
                .createdAt(LocalDateTime.now())
                .build();
        }
        return apiKeyRepository.save(apiKey);
    }

    @Transactional(readOnly = true)
    public List<ApiKey> getKeysForUser(User user) {
        return apiKeyRepository.findAllByUser(user);
    }

    @Transactional(readOnly = true)
    public String getDecryptedKey(UUID keyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
            .orElseThrow(() -> new ResourceNotFoundException("API Key not found: " + keyId));

        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied to this API key.");
        }

        return encryptionService.decrypt(apiKey.getEncryptedKey());
    }

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
