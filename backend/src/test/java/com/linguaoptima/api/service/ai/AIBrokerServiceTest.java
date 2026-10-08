package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.PendingAiTask;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.exception.AIServiceException;
import com.linguaoptima.api.exception.PaymentException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.repository.ApiKeyRepository;
import com.linguaoptima.api.repository.PendingAiTaskRepository;
import com.linguaoptima.api.service.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.client.RestTemplate;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIBrokerServiceTest {

    @Mock
    private GroqProvider groqProvider;
    @Mock
    private GeminiProvider geminiProvider;
    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private PendingAiTaskRepository pendingAiTaskRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AIBrokerService aiBrokerService;
    private User user;

    @BeforeEach
    void setUp() {
        when(groqProvider.getProviderName()).thenReturn("GROQ");
        when(geminiProvider.getProviderName()).thenReturn("GEMINI");

        aiBrokerService = new AIBrokerService(
            groqProvider,
            geminiProvider,
            apiKeyRepository,
            encryptionService,
            pendingAiTaskRepository,
            restTemplate,
            objectMapper,
            stringRedisTemplate
        );

        user = User.builder().id(UUID.randomUUID()).email("ai@lingua.com").build();
    }

    @Test
    void testGenerateTaskContentCacheHit() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(startsWith("ai_cache:"))).thenReturn("{\"cached\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"cached\": true}", result);
        verifyNoInteractions(groqProvider);
    }

    @Test
    void testGenerateTaskContentPrimarySuccess() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"generated\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"generated\": true}", result);
        verify(groqProvider).complete("prompt text");
        verify(valueOperations).set(anyString(), eq("{\"generated\": true}"), any());
    }

    @Test
    void testGenerateTaskContentFallbackToGemini() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenThrow(new RuntimeException("Groq 429 Rate limit"));
        when(geminiProvider.complete(anyString())).thenReturn("{\"gemini\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"gemini\": true}", result);
        verify(geminiProvider).complete("prompt text");
    }

    @Test
    void testGenerateTaskContentAllFailQueuesRequest() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenThrow(new RuntimeException("Groq down"));
        when(geminiProvider.complete(anyString())).thenThrow(new RuntimeException("Gemini down"));

        assertThrows(AIServiceException.class, () -> aiBrokerService.generateTaskContent("prompt", user));
        verify(pendingAiTaskRepository).save(any(PendingAiTask.class));
    }

    @Test
    void testScoreEssayNoCaching() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(geminiProvider.complete(anyString())).thenReturn("{\"score\": 8.0}");

        String result = aiBrokerService.scoreEssay("Essay body", user);
        assertEquals("{\"score\": 8.0}", result);
        // Verify cache was not checked or set for essay scoring
        verify(valueOperations, never()).get(startsWith("ai_cache:"));
    }

    @Test
    void testCheckGrammarPrimarySuccess() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"score\": 90.0}");

        String result = aiBrokerService.checkGrammar("student text", user);
        assertEquals("{\"score\": 90.0}", result);
    }

    @Test
    void testRateLimitExceededThrows() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + user.getId() + ":ai")).thenReturn(501L);

        assertThrows(QuotaExceededException.class, () -> aiBrokerService.generateTaskContent("prompt", user));
    }

    @Test
    void testRateLimitFirstRequestSetsExpire() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + user.getId() + ":ai")).thenReturn(1L);
        when(groqProvider.complete(anyString())).thenReturn("{\"ok\": true}");

        aiBrokerService.generateTaskContent("test", user);
        verify(stringRedisTemplate).expire(eq("rate_limit:" + user.getId() + ":ai"), any());
    }

    @Test
    void testCustomKeyOpenAISuccess() throws Exception {
        ApiKey key = ApiKey.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(AIProvider.OPENAI)
            .encryptedKey("encKey")
            .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(key));
        when(encryptionService.decrypt("encKey")).thenReturn("decrypted-openai-key");

        // mock restTemplate for OpenAIProvider
        org.springframework.http.ResponseEntity<String> resp =
            new org.springframework.http.ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"OpenAI output\"}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(resp);

        String result = aiBrokerService.generateTaskContent("Write prompt", user);
        assertEquals("OpenAI output", result);
    }

    @Test
    void testCustomKeyAnthropicSuccess() throws Exception {
        ApiKey key = ApiKey.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(AIProvider.ANTHROPIC)
            .encryptedKey("encKey")
            .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(key));
        when(encryptionService.decrypt("encKey")).thenReturn("decrypted-anthropic-key");

        org.springframework.http.ResponseEntity<String> resp =
            new org.springframework.http.ResponseEntity<>("{\"content\":[{\"text\":\"Claude output\"}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(eq("https://api.anthropic.com/v1/messages"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(resp);

        String result = aiBrokerService.scoreEssay("Essay body", user);
        assertEquals("Claude output", result);
    }

    @Test
    void testCustomKeyGeminiAndGroq() throws Exception {
        ApiKey keyGemini = ApiKey.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(AIProvider.GEMINI)
            .encryptedKey("encKey")
            .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(keyGemini));
        when(encryptionService.decrypt("encKey")).thenReturn("gemini-key");

        org.springframework.http.ResponseEntity<String> geminiResp =
            new org.springframework.http.ResponseEntity<>("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Gemini custom\"}]}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(contains("generativelanguage.googleapis.com"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(geminiResp);

        String res = aiBrokerService.checkGrammar("Sample text", user);
        assertEquals("Gemini custom", res);
    }

    @Test
    void testCustomKeyFailureThrowsPaymentException() {
        ApiKey key = ApiKey.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(AIProvider.GROQ)
            .encryptedKey("encKey")
            .build();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(key));
        when(encryptionService.decrypt("encKey")).thenReturn("bad-groq-key");
        when(restTemplate.exchange(contains("api.groq.com"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("401 Unauthorized Key"));

        assertThrows(PaymentException.class, () -> aiBrokerService.generateTaskContent("Prompt", user));
    }

    @Test
    void testFallbackPrimaryRetrySuccess() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString()))
            .thenThrow(new RuntimeException("Temporary network glitch"))
            .thenReturn("{\"recovered\": true}");
        when(geminiProvider.complete(anyString())).thenThrow(new RuntimeException("Gemini error"));

        String result = aiBrokerService.generateTaskContent("prompt", user);
        assertEquals("{\"recovered\": true}", result);
    }
}
