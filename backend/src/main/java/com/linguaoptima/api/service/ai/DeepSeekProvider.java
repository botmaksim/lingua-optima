/**
 * @file DeepSeekProvider.java
 * @brief DeepSeek API provider integration supporting BYOK user keys and deepseek-chat (DeepSeek-V3) inference.
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
 * @brief DeepSeek API provider integration supporting BYOK user keys and DeepSeek-V3.2 / R1 inference.
 */
@Component
public class DeepSeekProvider implements AIProvider {

    /** @brief Default DeepSeek model identifier. */
    public static final String DEFAULT_MODEL = "deepseek-chat";

    /** @brief Field representing api key in DeepSeekProvider. */
    private final String apiKey;
    /** @brief Field representing base url in DeepSeekProvider. */
    private final String baseUrl;
    /** @brief Field representing model name in DeepSeekProvider. */
    private final String modelName;
    /** @brief Field representing rest template in DeepSeekProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in DeepSeekProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a DeepSeekProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured DeepSeek API key.
     * @param baseUrl Configured DeepSeek API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public DeepSeekProvider(
        @Value("${app.ai.deepseek.api-key:dummy-deepseek-key}") String apiKey,
        @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this(apiKey, baseUrl, DEFAULT_MODEL, restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a DeepSeekProvider with custom model name and base URL.
     * @param apiKey DeepSeek API key.
     * @param baseUrl DeepSeek API base URL.
     * @param modelName Selected model identifier.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public DeepSeekProvider(
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
     * @brief Constructs a DeepSeekProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied DeepSeek API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public DeepSeekProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://api.deepseek.com", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a DeepSeekProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public DeepSeekProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-deepseek-key", "https://api.deepseek.com", restTemplate, objectMapper);
    }

    /**
     * @brief Returns provider identifier name "DEEPSEEK".
     * @return "DEEPSEEK" string.
     */
    @Override
    public String getProviderName() {
        return "DEEPSEEK";
    }

    /**
     * @brief Sends chat completion request to DeepSeek API using the configured model.
     * @param prompt Input prompt text.
     * @return Model response text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", modelName,
            "messages", List.of(
                Map.of("role", "system", "content", "You are an English language testing expert. Respond strictly with valid JSON."),
                Map.of("role", "user", "content", prompt)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }

        throw new RuntimeException("DeepSeek API returned status: " + response.getStatusCode());
    }
}
