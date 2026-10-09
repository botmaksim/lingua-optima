/**
 * @file AIProvidersTest.java
 * @brief Unit and slice test suite for AIProviders.
 */
package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for AIProviders.
 */
@ExtendWith(MockitoExtension.class)
class AIProvidersTest {

    /** @brief Test fixture or mock dependency for rest template. */
    @Mock
    private RestTemplate restTemplate;

    /** @brief Test fixture or mock dependency for object mapper. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @brief Verifies unit test scenario: groq provider success.
     */
    @Test
    void testGroqProviderSuccess() throws Exception {
        GroqProvider provider = new GroqProvider("groq-key", restTemplate, objectMapper);
        assertEquals("GROQ", provider.getProviderName());

        String json = "{\"choices\":[{\"message\":{\"content\":\"Groq response\"}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Groq response", res);
    }

    /**
     * @brief Verifies unit test scenario: groq provider http error throws.
     */
    @Test
    void testGroqProviderHttpErrorThrows() {
        GroqProvider provider = new GroqProvider("groq-key", restTemplate, objectMapper);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(RuntimeException.class, () -> provider.complete("hello"));
    }

    /**
     * @brief Verifies unit test scenario: groq provider missing or blank key throws.
     */
    @Test
    void testGroqProviderMissingKeyThrows() {
        GroqProvider providerNull = new GroqProvider(null, restTemplate, objectMapper);
        assertThrows(IllegalStateException.class, () -> providerNull.complete("hello"));

        GroqProvider providerBlank = new GroqProvider("   ", restTemplate, objectMapper);
        assertThrows(IllegalStateException.class, () -> providerBlank.complete("hello"));
    }

    /**
     * @brief Verifies unit test scenario: gemini provider missing or blank key throws.
     */
    @Test
    void testGeminiProviderMissingKeyThrows() {
        GeminiProvider providerNull = new GeminiProvider(null, restTemplate, objectMapper);
        assertThrows(IllegalStateException.class, () -> providerNull.complete("hello"));

        GeminiProvider providerBlank = new GeminiProvider("   ", restTemplate, objectMapper);
        assertThrows(IllegalStateException.class, () -> providerBlank.complete("hello"));
    }

    /**
     * @brief Verifies unit test scenario: gemini provider invalid api key aborts fallback loop.
     */
    @Test
    void testGeminiProviderInvalidApiKeyAbortsFallback() {
        GeminiProvider provider = new GeminiProvider("gemini-key", restTemplate, objectMapper);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("API_KEY_INVALID: Key is invalid"));

        assertThrows(RuntimeException.class, () -> provider.complete("hello"));
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(1))
            .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }

    /**
     * @brief Verifies unit test scenario: gemini provider success.
     */
    @Test
    void testGeminiProviderSuccess() throws Exception {
        GeminiProvider provider = new GeminiProvider("gemini-key", restTemplate, objectMapper);
        assertEquals("GEMINI", provider.getProviderName());

        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Gemini response\"}]}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Gemini response", res);
    }

    /**
     * @brief Verifies unit test scenario: gemini provider fallback on model failure.
     */
    @Test
    void testGeminiProviderFallbackOnModelFailure() throws Exception {
        GeminiProvider provider = new GeminiProvider("gemini-key", "https://generativelanguage.googleapis.com", "gemini-3.6-flash", restTemplate, objectMapper);
        assertEquals("GEMINI", provider.getProviderName());

        String json = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Fallback response\"}]}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("First model 429 quota"))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Fallback response", res);
    }

    /**
     * @brief Verifies unit test scenario: gemini provider all models exception throws.
     */
    @Test
    void testGeminiProviderAllModelsExceptionThrows() {
        GeminiProvider provider = new GeminiProvider("gemini-key", restTemplate, objectMapper);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RuntimeException("All models fail"));

        assertThrows(RuntimeException.class, () -> provider.complete("hello"));
    }

    /**
     * @brief Verifies unit test scenario: open aiprovider success.
     */
    @Test
    void testOpenAIProviderSuccess() throws Exception {
        OpenAIProvider provider = new OpenAIProvider("openai-key", restTemplate, objectMapper);
        assertEquals("OPENAI", provider.getProviderName());

        String json = "{\"choices\":[{\"message\":{\"content\":\"OpenAI response\"}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("OpenAI response", res);
    }

    /**
     * @brief Verifies unit test scenario: anthropic provider success.
     */
    @Test
    void testAnthropicProviderSuccess() throws Exception {
        AnthropicProvider provider = new AnthropicProvider("claude-key", restTemplate, objectMapper);
        assertEquals("ANTHROPIC", provider.getProviderName());

        String json = "{\"content\":[{\"text\":\"Claude response\"}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Claude response", res);
    }

    /**
     * @brief Verifies unit test scenario: deepseek provider success.
     */
    @Test
    void testDeepSeekProviderSuccess() throws Exception {
        DeepSeekProvider provider = new DeepSeekProvider("deepseek-key", restTemplate, objectMapper);
        assertEquals("DEEPSEEK", provider.getProviderName());

        String json = "{\"choices\":[{\"message\":{\"content\":\"DeepSeek response\"}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("DeepSeek response", res);
    }

    /**
     * @brief Verifies unit test scenario: qwen provider success.
     */
    @Test
    void testQwenProviderSuccess() throws Exception {
        QwenProvider provider = new QwenProvider("qwen-key", restTemplate, objectMapper);
        assertEquals("QWEN", provider.getProviderName());

        String json = "{\"choices\":[{\"message\":{\"content\":\"Qwen response\"}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Qwen response", res);
    }

    /**
     * @brief Verifies unit test scenario: kimi provider success.
     */
    @Test
    void testKimiProviderSuccess() throws Exception {
        KimiProvider provider = new KimiProvider("kimi-key", restTemplate, objectMapper);
        assertEquals("KIMI", provider.getProviderName());

        String json = "{\"choices\":[{\"message\":{\"content\":\"Kimi response\"}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));

        String res = provider.complete("hello");
        assertEquals("Kimi response", res);
    }

    /**
     * @brief Verifies unit test scenario: default constructors and non-2xx HTTP error branches.
     */
    @Test
    void testProvidersDefaultConstructorsAndHttpErrors() {
        GroqProvider groq = new GroqProvider("groq-key", restTemplate, objectMapper);
        GeminiProvider gemini = new GeminiProvider("gemini-key", restTemplate, objectMapper);
        OpenAIProvider openai = new OpenAIProvider(restTemplate, objectMapper);
        AnthropicProvider anthropic = new AnthropicProvider(restTemplate, objectMapper);
        DeepSeekProvider deepseek = new DeepSeekProvider(restTemplate, objectMapper);
        QwenProvider qwen = new QwenProvider(restTemplate, objectMapper);
        KimiProvider kimi = new KimiProvider(restTemplate, objectMapper);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.BAD_GATEWAY));

        assertThrows(RuntimeException.class, () -> gemini.complete("prompt"));
        assertThrows(RuntimeException.class, () -> openai.complete("prompt"));
        assertThrows(RuntimeException.class, () -> anthropic.complete("prompt"));
        assertThrows(RuntimeException.class, () -> deepseek.complete("prompt"));
        assertThrows(RuntimeException.class, () -> qwen.complete("prompt"));
        assertThrows(RuntimeException.class, () -> kimi.complete("prompt"));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertThrows(RuntimeException.class, () -> groq.complete("prompt"));
        assertThrows(RuntimeException.class, () -> gemini.complete("prompt"));
        assertThrows(RuntimeException.class, () -> openai.complete("prompt"));
        assertThrows(RuntimeException.class, () -> anthropic.complete("prompt"));
        assertThrows(RuntimeException.class, () -> deepseek.complete("prompt"));
        assertThrows(RuntimeException.class, () -> qwen.complete("prompt"));
        assertThrows(RuntimeException.class, () -> kimi.complete("prompt"));
    }
}

