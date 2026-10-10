/**
 * @file AIBrokerServiceTest.java
 * @brief Unit and slice test suite for AIBrokerService.
 */
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

/**
 * @brief Unit and slice test suite for AIBrokerService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIBrokerServiceTest {

    /** @brief Test fixture or mock dependency for groq provider. */
    @Mock
    private GroqProvider groqProvider;
    /** @brief Test fixture or mock dependency for gemini provider. */
    @Mock
    private GeminiProvider geminiProvider;
    /** @brief Test fixture or mock dependency for api key repository. */
    @Mock
    private ApiKeyRepository apiKeyRepository;
    /** @brief Test fixture or mock dependency for encryption service. */
    @Mock
    private EncryptionService encryptionService;
    /** @brief Test fixture or mock dependency for pending ai task repository. */
    @Mock
    private PendingAiTaskRepository pendingAiTaskRepository;
    /** @brief Test fixture or mock dependency for rest template. */
    @Mock
    private RestTemplate restTemplate;
    /** @brief Test fixture or mock dependency for string redis template. */
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    /** @brief Test fixture or mock dependency for value operations. */
    @Mock
    private ValueOperations<String, String> valueOperations;

    /** @brief Test fixture or mock dependency for object mapper. */
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** @brief Test fixture or mock dependency for ai broker service. */
    private AIBrokerService aiBrokerService;
    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in AIBrokerServiceTest.
     */
    @BeforeEach
    void setUp() {
        when(groqProvider.getProviderName()).thenReturn("GROQ");
        when(geminiProvider.getProviderName()).thenReturn("GEMINI");
        lenient().when(groqProvider.isConfigured()).thenReturn(true);
        lenient().when(geminiProvider.isConfigured()).thenReturn(true);

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

    /**
     * @brief Verifies unit test scenario: generate task content no caching.
     */
    @Test
    void testGenerateTaskContentNoCaching() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"generated\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"generated\": true}", result);
        verify(groqProvider).complete("prompt text");
        verify(valueOperations, never()).get(startsWith("ai_cache:"));
        verify(valueOperations, never()).set(startsWith("ai_cache:"), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: generate task content primary success.
     */
    @Test
    void testGenerateTaskContentPrimarySuccess() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"generated\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"generated\": true}", result);
        verify(groqProvider).complete("prompt text");
    }

    /**
     * @brief Verifies unit test scenario: generate task content fallback to gemini.
     */
    @Test
    void testGenerateTaskContentFallbackToGemini() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenThrow(new RuntimeException("Groq 429 Rate limit"));
        when(geminiProvider.complete(anyString())).thenReturn("{\"gemini\": true}");

        String result = aiBrokerService.generateTaskContent("prompt text", user);
        assertEquals("{\"gemini\": true}", result);
        verify(geminiProvider).complete("prompt text");
    }

    /**
     * @brief Verifies unit test scenario: generate task content all fail queues request.
     */
    @Test
    void testGenerateTaskContentAllFailQueuesRequest() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenThrow(new RuntimeException("Groq down"));
        when(geminiProvider.complete(anyString())).thenThrow(new RuntimeException("Gemini down"));

        assertThrows(AIServiceException.class, () -> aiBrokerService.generateTaskContent("prompt", user));
        verify(pendingAiTaskRepository).save(any(PendingAiTask.class));
    }

    /**
     * @brief Verifies unit test scenario: score essay no caching.
     */
    @Test
    void testScoreEssayNoCaching() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(geminiProvider.complete(anyString())).thenReturn("{\"score\": 8.0}");

        String result = aiBrokerService.scoreEssay("Essay body", user);
        assertEquals("{\"score\": 8.0}", result);
        verify(valueOperations, never()).get(startsWith("ai_cache:"));
    }

    /**
     * @brief Verifies unit test scenario: check grammar primary success.
     */
    @Test
    void testCheckGrammarPrimarySuccess() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"score\": 90.0}");

        String result = aiBrokerService.checkGrammar("student text", user);
        assertEquals("{\"score\": 90.0}", result);
    }

    /**
     * @brief Verifies unit test scenario: rate limit exceeded throws.
     */
    @Test
    void testRateLimitExceededThrows() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + user.getId() + ":ai")).thenReturn(501L);

        assertThrows(QuotaExceededException.class, () -> aiBrokerService.generateTaskContent("prompt", user));
    }

    /**
     * @brief Verifies unit test scenario: rate limit first request sets expire.
     */
    @Test
    void testRateLimitFirstRequestSetsExpire() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:" + user.getId() + ":ai")).thenReturn(1L);
        when(groqProvider.complete(anyString())).thenReturn("{\"ok\": true}");

        aiBrokerService.generateTaskContent("test", user);
        verify(stringRedisTemplate).expire(eq("rate_limit:" + user.getId() + ":ai"), any());
    }

    /**
     * @brief Verifies unit test scenario: custom key open aisuccess.
     */
    @Test
    void testCustomKeyOpenAISuccess() throws Exception {
        ApiKey key = ApiKey.builder()
            .id(UUID.randomUUID())
            .user(user)
            .provider(AIProvider.OPENAI)
            .encryptedKey("encKey")
            .build();

        user.setPreferredProvider(AIProvider.OPENAI);
        user.setPreferredModel("gpt-5");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(key));
        when(encryptionService.decrypt("encKey")).thenReturn("decrypted-openai-key");

        org.springframework.http.ResponseEntity<String> resp =
            new org.springframework.http.ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"OpenAI output\"}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(eq("https://api.openai.com/v1/chat/completions"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(resp);

        String result = aiBrokerService.generateTaskContent("Write prompt", user);
        assertEquals("OpenAI output", result);
    }

    /**
     * @brief Verifies unit test scenario: custom key anthropic success.
     */
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

    /**
     * @brief Verifies unit test scenario: custom key gemini and groq.
     */
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

    /**
     * @brief Verifies unit test scenario: custom keys for Chinese providers DeepSeek, Qwen, and Kimi.
     */
    @Test
    void testCustomKeyChineseProviders() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        // DeepSeek
        ApiKey keyDeepSeek = ApiKey.builder().id(UUID.randomUUID()).user(user).provider(AIProvider.DEEPSEEK).encryptedKey("encKey").build();
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(keyDeepSeek));
        when(encryptionService.decrypt("encKey")).thenReturn("ds-key");
        org.springframework.http.ResponseEntity<String> dsResp =
            new org.springframework.http.ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"DS response\"}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(contains("deepseek.com"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(dsResp);
        assertEquals("DS response", aiBrokerService.scoreEssay("Essay", user));

        // Qwen
        ApiKey keyQwen = ApiKey.builder().id(UUID.randomUUID()).user(user).provider(AIProvider.QWEN).encryptedKey("encKey").build();
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(keyQwen));
        org.springframework.http.ResponseEntity<String> qwenResp =
            new org.springframework.http.ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"Qwen response\"}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(contains("dashscope"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(qwenResp);
        assertEquals("Qwen response", aiBrokerService.scoreEssay("Essay", user));

        // Kimi
        ApiKey keyKimi = ApiKey.builder().id(UUID.randomUUID()).user(user).provider(AIProvider.KIMI).encryptedKey("encKey").build();
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of(keyKimi));
        org.springframework.http.ResponseEntity<String> kimiResp =
            new org.springframework.http.ResponseEntity<>("{\"choices\":[{\"message\":{\"content\":\"Kimi response\"}}]}", org.springframework.http.HttpStatus.OK);
        when(restTemplate.exchange(contains("moonshot.cn"), eq(org.springframework.http.HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(String.class)))
            .thenReturn(kimiResp);
        assertEquals("Kimi response", aiBrokerService.scoreEssay("Essay", user));
    }

    /**
     * @brief Verifies unit test scenario: custom key failure throws payment exception.
     */
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

    /**
     * @brief Verifies unit test scenario: fallback primary retry success.
     */
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

    /**
     * @brief Verifies unit test scenario: Redis rate-limit exceptions and null Redis/user fallbacks.
     */
    @Test
    void testRedisExceptionsAndNullUserBranches() throws Exception {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis inc error"));
        when(apiKeyRepository.findAllByUserId(user.getId())).thenReturn(List.of());
        when(groqProvider.complete(anyString())).thenReturn("{\"ok\": true}");

        String res = aiBrokerService.generateTaskContent("prompt", user);
        assertEquals("{\"ok\": true}", res);

        AIBrokerService noRedisBroker = new AIBrokerService(
            groqProvider,
            geminiProvider,
            apiKeyRepository,
            encryptionService,
            pendingAiTaskRepository,
            restTemplate,
            objectMapper,
            null
        );
        assertEquals("{\"ok\": true}", noRedisBroker.generateTaskContent("prompt", null));
        assertEquals("{\"ok\": true}", noRedisBroker.generateTaskContent("prompt", user));

        User unpersistedUser = User.builder().id(null).build();
        reset(valueOperations, groqProvider, geminiProvider);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(null);
        when(groqProvider.complete(anyString())).thenReturn(null);
        assertNull(aiBrokerService.generateTaskContent("fresh-prompt", user));
        assertNull(aiBrokerService.generateTaskContent("fresh-prompt", unpersistedUser));

        when(groqProvider.complete(anyString())).thenThrow(new RuntimeException("Primary down"));
        when(geminiProvider.complete(anyString())).thenThrow(new RuntimeException("Secondary down"));
        assertThrows(AIServiceException.class, () -> aiBrokerService.generateTaskContent("fail", null));
        assertThrows(AIServiceException.class, () -> aiBrokerService.generateTaskContent("fail", unpersistedUser));
    }
}

