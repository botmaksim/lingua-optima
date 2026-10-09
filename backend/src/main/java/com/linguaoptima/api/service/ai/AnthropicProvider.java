package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @file AnthropicProvider.java
 * @brief Anthropic Claude API provider integration supporting BYOK user keys and claude-3-5-sonnet.
 */
@Component
public class AnthropicProvider implements AIProvider {

    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs an AnthropicProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public AnthropicProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-claude-key", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an AnthropicProvider with explicit API key.
     * @param apiKey Anthropic API secret key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public AnthropicProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @brief Returns provider identifier name "ANTHROPIC".
     * @return "ANTHROPIC" string.
     */
    @Override
    public String getProviderName() {
        return "ANTHROPIC";
    }

    /**
     * @brief Sends message completion request to Anthropic API using claude-3-5-sonnet-20241022.
     * @param prompt Input prompt text.
     * @return Model completion text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = "https://api.anthropic.com/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
            "model", "claude-3-5-sonnet-20241022",
            "max_tokens", 2048,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("content").get(0).path("text").asText();
        }

        throw new RuntimeException("Anthropic API returned status: " + response.getStatusCode());
    }
}
