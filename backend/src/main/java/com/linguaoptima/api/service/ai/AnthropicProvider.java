/**
 * @file AnthropicProvider.java
 * @brief Anthropic Claude API provider integration supporting BYOK user keys and Claude Fable 5.1 / Opus 5.5 / Sonnet 5.5 inference.
 */
package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @brief Anthropic Claude API provider integration supporting BYOK user keys and Claude Fable 5.1 / Opus 5.5 / Sonnet 5.5 inference.
 */
@Component
public class AnthropicProvider implements AIProvider {

    /** @brief Default Anthropic model identifier. */
    public static final String DEFAULT_MODEL = "claude-sonnet-5-5";

    /** @brief Field representing api key in AnthropicProvider. */
    private final String apiKey;
    /** @brief Field representing base url in AnthropicProvider. */
    private final String baseUrl;
    /** @brief Field representing model name in AnthropicProvider. */
    private final String modelName;
    /** @brief Field representing rest template in AnthropicProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in AnthropicProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs an AnthropicProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured Anthropic API key.
     * @param baseUrl Configured Anthropic API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public AnthropicProvider(
        @Value("${app.ai.anthropic.api-key:dummy-claude-key}") String apiKey,
        @Value("${app.ai.anthropic.base-url:https://api.anthropic.com/v1}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this(apiKey, baseUrl, DEFAULT_MODEL, restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an AnthropicProvider with custom model name and base URL.
     * @param apiKey Anthropic API key.
     * @param baseUrl Anthropic API base URL.
     * @param modelName Selected model identifier.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public AnthropicProvider(
        String apiKey,
        String baseUrl,
        String modelName,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = Objects.requireNonNullElse(modelName, DEFAULT_MODEL);
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @brief Constructs an AnthropicProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied Anthropic API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public AnthropicProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://api.anthropic.com/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an AnthropicProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public AnthropicProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-claude-key", "https://api.anthropic.com/v1", restTemplate, objectMapper);
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
     * @brief Sends message completion request to Anthropic API using the configured model.
     * @param prompt Input prompt text.
     * @return Model completion text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
            "model", modelName,
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
