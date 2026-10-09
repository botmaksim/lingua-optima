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
 * @file AIProvidersTest.java
 * @brief Unit and slice test suite for AIProviders.
 */
@ExtendWith(MockitoExtension.class)
class AIProvidersTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Test
    void testGroqProviderHttpErrorThrows() {
        GroqProvider provider = new GroqProvider("groq-key", restTemplate, objectMapper);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(RuntimeException.class, () -> provider.complete("hello"));
    }

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
}
