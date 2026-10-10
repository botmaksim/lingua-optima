/**
 * @file GeminiProvider.java
 * @brief Google Gemini API provider utilizing Gemini 3.8 Flash and Gemini 3.x models for essay evaluations and generation.
 */
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
import java.util.Objects;

/**
 * @brief Google Gemini API provider utilizing Gemini 3.8 Flash and Gemini 3.x models for essay evaluations and generation.
 */
@Slf4j
@Component
public class GeminiProvider implements AIProvider {

    /** @brief Default Gemini model identifier. */
    public static final String DEFAULT_MODEL = "gemini-3.6-flash";

    /** @brief Field representing api key in GeminiProvider. */
    private final String apiKey;
    /** @brief Field representing base url in GeminiProvider. */
    private final String baseUrl;
    /** @brief Field representing model name in GeminiProvider. */
    private final String modelName;
    /** @brief Field representing rest template in GeminiProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in GeminiProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a GeminiProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured Gemini API key.
     * @param baseUrl Configured Gemini API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public GeminiProvider(
        @Value("${app.ai.gemini.api-key:dummy-gemini-key}") String apiKey,
        @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this(apiKey, baseUrl, DEFAULT_MODEL, restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a GeminiProvider with custom model name and base URL.
     * @param apiKey Gemini API key.
     * @param baseUrl Gemini API base URL.
     * @param modelName Selected model identifier.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public GeminiProvider(
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
     * @brief Constructs a GeminiProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied Gemini API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public GeminiProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://generativelanguage.googleapis.com", restTemplate, objectMapper);
    }

    /**
     * @brief Returns provider identifier name "GEMINI".
     * @return "GEMINI" string.
     */
    @Override
    public String getProviderName() {
        return "GEMINI";
    }

    /**
     * @brief Checks if Gemini API key is present and not a dummy/mock placeholder.
     * @return true if valid key is set.
     */
    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("mock") && !apiKey.contains("dummy");
    }

    /**
     * @brief Sends content generation request to Google Gemini API using the configured model.
     * @param prompt Input prompt text.
     * @return Generated model candidate text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        List<String> modelsToTry = new java.util.ArrayList<>();
        if (modelName != null && !modelName.isBlank()) {
            modelsToTry.add(modelName);
        }
        for (String m : List.of("gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-3.1-flash-lite", "gemini-flash-lite-latest", "gemini-flash-latest")) {
            if (!modelsToTry.contains(m)) {
                modelsToTry.add(m);
            }
        }

        Exception lastException = null;
        for (String model : modelsToTry) {
            try {
                String url = baseUrl.replaceAll("/+$", "") + "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> body = Map.of(
                    "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                    )
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                }
                lastException = new RuntimeException("Gemini API returned error status: " + response.getStatusCode());
            } catch (Exception e) {
                log.warn("Gemini model {} failed ({}), attempting fallback if available...", model, e.getMessage());
                if (e.getMessage() != null && e.getMessage().contains("API_KEY_INVALID")) {
                    throw e;
                }
                lastException = e;
            }
        }

        throw lastException;
    }
}
