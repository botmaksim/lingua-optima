package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @file GroqProvider.java
 * @brief Ultra-low-latency Groq LPU inference provider using Llama 3.1 models.
 */
@Slf4j
@Component
public class GroqProvider implements AIProvider {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a GroqProvider with injected API key and HTTP client components.
     * @param apiKey Configured Groq API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public GroqProvider(
        @Value("${app.ai.groq.api-key:dummy-groq-key}") String apiKey,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @brief Returns provider identifier name "GROQ".
     * @return "GROQ" string.
     */
    @Override
    public String getProviderName() {
        return "GROQ";
    }

    /**
     * @brief Sends chat completion request to Groq API using llama-3.1-70b-versatile.
     * @param prompt User prompt text.
     * @return Model completion text content.
     * @throws Exception if HTTP exchange fails or non-2xx status code is received.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.1-70b-versatile",
            "messages", List.of(
                Map.of("role", "system", "content", "You are an AI language learning assistant. Always return valid JSON as requested."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.7
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }

        throw new RuntimeException("Groq API returned error status: " + response.getStatusCode());
    }
}
