/**
 * @file QwenProvider.java
 * @brief Alibaba Cloud Qwen (DashScope) API provider integration supporting BYOK user keys and qwen-plus inference.
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
 * @brief Alibaba Cloud Qwen (DashScope) API provider integration supporting BYOK user keys and Qwen 3 / Qwen-Max inference.
 */
@Component
public class QwenProvider implements AIProvider {

    /** @brief Default Qwen model identifier. */
    public static final String DEFAULT_MODEL = "qwen3-235b-a22b";

    /** @brief Field representing api key in QwenProvider. */
    private final String apiKey;
    /** @brief Field representing base url in QwenProvider. */
    private final String baseUrl;
    /** @brief Field representing model name in QwenProvider. */
    private final String modelName;
    /** @brief Field representing rest template in QwenProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in QwenProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a QwenProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured Qwen API key.
     * @param baseUrl Configured Qwen API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public QwenProvider(
        @Value("${app.ai.qwen.api-key:dummy-qwen-key}") String apiKey,
        @Value("${app.ai.qwen.base-url:https://dashscope-intl.aliyuncs.com/compatible-mode/v1}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this(apiKey, baseUrl, DEFAULT_MODEL, restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a QwenProvider with custom model name and base URL.
     * @param apiKey Qwen API key.
     * @param baseUrl Qwen API base URL.
     * @param modelName Selected model identifier.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public QwenProvider(
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
     * @brief Constructs a QwenProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied Qwen API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public QwenProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://dashscope-intl.aliyuncs.com/compatible-mode/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a QwenProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public QwenProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-qwen-key", "https://dashscope-intl.aliyuncs.com/compatible-mode/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Returns provider identifier name "QWEN".
     * @return "QWEN" string.
     */
    @Override
    public String getProviderName() {
        return "QWEN";
    }

    /**
     * @brief Sends chat completion request to Qwen (DashScope) API using the configured model.
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

        throw new RuntimeException("Qwen API returned status: " + response.getStatusCode());
    }
}
