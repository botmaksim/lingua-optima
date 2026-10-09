/**
 * @file OpenAIProvider.java
 * @brief OpenAI API provider integration supporting BYOK user keys and GPT-6 Astra / GPT-6.1 Sol / GPT-6 Luna inference.
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
 * @brief OpenAI API provider integration supporting BYOK user keys and GPT-6 Astra / GPT-6.1 Sol / GPT-6 Luna inference.
 */
@Component
public class OpenAIProvider implements AIProvider {

    /** @brief Default OpenAI model identifier. */
    public static final String DEFAULT_MODEL = "gpt-6.1-sol";

    /** @brief Field representing api key in OpenAIProvider. */
    private final String apiKey;
    /** @brief Field representing base url in OpenAIProvider. */
    private final String baseUrl;
    /** @brief Field representing model name in OpenAIProvider. */
    private final String modelName;
    /** @brief Field representing rest template in OpenAIProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in OpenAIProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs an OpenAIProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured OpenAI API key.
     * @param baseUrl Configured OpenAI API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public OpenAIProvider(
        @Value("${app.ai.openai.api-key:dummy-openai-key}") String apiKey,
        @Value("${app.ai.openai.base-url:https://api.openai.com/v1}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this(apiKey, baseUrl, DEFAULT_MODEL, restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an OpenAIProvider with custom model name and base URL.
     * @param apiKey OpenAI API key.
     * @param baseUrl OpenAI API base URL.
     * @param modelName Selected model identifier.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public OpenAIProvider(
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
     * @brief Constructs an OpenAIProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied OpenAI API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public OpenAIProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://api.openai.com/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an OpenAIProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public OpenAIProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-openai-key", "https://api.openai.com/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Returns provider identifier name "OPENAI".
     * @return "OPENAI" string.
     */
    @Override
    public String getProviderName() {
        return "OPENAI";
    }

    /**
     * @brief Sends chat completion request to OpenAI API using the configured model.
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

        throw new RuntimeException("OpenAI API returned status: " + response.getStatusCode());
    }
}
