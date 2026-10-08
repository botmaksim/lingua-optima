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

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

    @Mock
    private ApiKeyService apiKeyService;

    @InjectMocks
    private ApiKeyController apiKeyController;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testGetApiKeys() {
        ApiKey k = ApiKey.builder().id(UUID.randomUUID()).build();
        when(apiKeyService.getKeysForUser(user)).thenReturn(List.of(k));

        ResponseEntity<List<ApiKey>> res = apiKeyController.getApiKeys(user);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testAddApiKey() {
        CreateApiKeyRequest req = CreateApiKeyRequest.builder().provider(AIProvider.OPENAI).rawKey("key").build();
        ResponseEntity<Void> res = apiKeyController.addApiKey(user, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(apiKeyService).saveKey(req, user);
    }

    @Test
    void testDeleteApiKey() {
        UUID id = UUID.randomUUID();
        ResponseEntity<Void> res = apiKeyController.deleteApiKey(id, user);
        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(apiKeyService).deleteKey(id, user);
    }
}
