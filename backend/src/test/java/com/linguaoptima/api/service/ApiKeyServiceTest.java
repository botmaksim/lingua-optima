/**
 * @file ApiKeyServiceTest.java
 * @brief Unit and slice test suite for ApiKeyService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.dto.request.CreateApiKeyRequest;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for ApiKeyService.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    /** @brief Test fixture or mock dependency for api key repository. */
    @Mock
    private ApiKeyRepository apiKeyRepository;

    /** @brief Test fixture or mock dependency for encryption service. */
    @Mock
    private EncryptionService encryptionService;

    /** @brief Test fixture or mock dependency for api key service. */
    @InjectMocks
    private ApiKeyService apiKeyService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;
    /** @brief Test fixture or mock dependency for other user. */
    private User otherUser;
    /** @brief Test fixture or mock dependency for api key. */
    private ApiKey apiKey;

    /**
     * @brief Initializes test fixtures and mock state before each test in ApiKeyServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("keyowner@lingua.com").build();
        otherUser = User.builder().id(UUID.randomUUID()).build();
        apiKey = ApiKey.builder().id(UUID.randomUUID()).user(user).provider(AIProvider.OPENAI).encryptedKey("enc123").build();
    }

    /**
     * @brief Verifies unit test scenario: save key new.
     */
    @Test
    void testSaveKeyNew() {
        CreateApiKeyRequest req = CreateApiKeyRequest.builder().provider(AIProvider.OPENAI).rawKey("sk-secret").build();
        when(encryptionService.encrypt("sk-secret")).thenReturn("enc123");
        when(apiKeyRepository.findByUserAndProvider(user, AIProvider.OPENAI)).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKey saved = apiKeyService.saveKey(req, user);
        assertNotNull(saved);
        assertEquals("enc123", saved.getEncryptedKey());
    }

    /**
     * @brief Verifies unit test scenario: save key update existing.
     */
    @Test
    void testSaveKeyUpdateExisting() {
        CreateApiKeyRequest req = CreateApiKeyRequest.builder().provider(AIProvider.OPENAI).rawKey("sk-new-secret").build();
        when(encryptionService.encrypt("sk-new-secret")).thenReturn("encNew");
        when(apiKeyRepository.findByUserAndProvider(user, AIProvider.OPENAI)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKey updated = apiKeyService.saveKey(req, user);
        assertEquals("encNew", updated.getEncryptedKey());
    }

    /**
     * @brief Verifies unit test scenario: get keys for user.
     */
    @Test
    void testGetKeysForUser() {
        when(apiKeyRepository.findAllByUser(user)).thenReturn(List.of(apiKey));
        assertEquals(1, apiKeyService.getKeysForUser(user).size());
    }

    /**
     * @brief Verifies unit test scenario: get decrypted key.
     */
    @Test
    void testGetDecryptedKey() {
        when(apiKeyRepository.findById(apiKey.getId())).thenReturn(Optional.of(apiKey));
        when(encryptionService.decrypt("enc123")).thenReturn("sk-decrypted");

        String decrypted = apiKeyService.getDecryptedKey(apiKey.getId(), user);
        assertEquals("sk-decrypted", decrypted);

        assertThrows(ForbiddenException.class, () -> apiKeyService.getDecryptedKey(apiKey.getId(), otherUser));
    }

    /**
     * @brief Verifies unit test scenario: delete key.
     */
    @Test
    void testDeleteKey() {
        when(apiKeyRepository.findById(apiKey.getId())).thenReturn(Optional.of(apiKey));
        assertDoesNotThrow(() -> apiKeyService.deleteKey(apiKey.getId(), user));
        verify(apiKeyRepository).delete(apiKey);

        assertThrows(ForbiddenException.class, () -> apiKeyService.deleteKey(apiKey.getId(), otherUser));
    }

    /**
     * @brief Verifies unit test scenario: get decrypted key not found throws.
     */
    @Test
    void testGetDecryptedKeyNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> apiKeyService.getDecryptedKey(id, user));
    }

    /**
     * @brief Verifies unit test scenario: delete key not found throws.
     */
    @Test
    void testDeleteKeyNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(apiKeyRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> apiKeyService.deleteKey(id, user));
    }
}
