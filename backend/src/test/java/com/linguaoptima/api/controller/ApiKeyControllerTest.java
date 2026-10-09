/**
 * @file ApiKeyControllerTest.java
 * @brief Unit and slice test suite for ApiKeyController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.dto.request.CreateApiKeyRequest;
import com.linguaoptima.api.service.ApiKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for ApiKeyController.
 */
@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    /** @brief Test fixture or mock dependency for api key service. */
    @Mock
    private ApiKeyService apiKeyService;

    /** @brief Test fixture or mock dependency for api key controller. */
    @InjectMocks
    private ApiKeyController apiKeyController;

    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in ApiKeyControllerTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    /**
     * @brief Verifies unit test scenario: get api keys.
     */
    @Test
    void testGetApiKeys() {
        ApiKey k = ApiKey.builder().id(UUID.randomUUID()).build();
        when(apiKeyService.getKeysForUser(user)).thenReturn(List.of(k));

        ResponseEntity<List<ApiKey>> res = apiKeyController.getApiKeys(user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    /**
     * @brief Verifies unit test scenario: add api key.
     */
    @Test
    void testAddApiKey() {
        CreateApiKeyRequest req = CreateApiKeyRequest.builder().provider(AIProvider.OPENAI).rawKey("key").build();
        ResponseEntity<Void> res = apiKeyController.addApiKey(user, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(apiKeyService).saveKey(req, user);
    }

    /**
     * @brief Verifies unit test scenario: delete api key.
     */
    @Test
    void testDeleteApiKey() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> res = apiKeyController.deleteApiKey(id, user);
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(apiKeyService).deleteKey(id, user);
    }
}
