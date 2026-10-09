/**
 * @file KimiProvider.java
 * @brief Moonshot AI Kimi API provider integration supporting BYOK user keys and moonshot-v1-8k inference.
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

/**
 * @brief Moonshot AI Kimi API provider integration supporting BYOK user keys and moonshot-v1-8k inference.
 */
@Component
public class KimiProvider implements AIProvider {

    /** @brief Field representing api key in KimiProvider. */
    private final String apiKey;
    /** @brief Field representing base url in KimiProvider. */
    private final String baseUrl;
    /** @brief Field representing rest template in KimiProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in KimiProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a KimiProvider with injected API key, base URL, and HTTP client components.
     * @param apiKey Configured Kimi API key.
     * @param baseUrl Configured Kimi API base URL.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public KimiProvider(
        @Value("${app.ai.kimi.api-key:dummy-kimi-key}") String apiKey,
        @Value("${app.ai.kimi.base-url:https://api.moonshot.cn/v1}") String baseUrl,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @brief Constructs a KimiProvider for BYOK user keys with default API base URL.
     * @param apiKey User-supplied Kimi API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public KimiProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this(apiKey, "https://api.moonshot.cn/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs a KimiProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public KimiProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-kimi-key", "https://api.moonshot.cn/v1", restTemplate, objectMapper);
    }

    /**
     * @brief Returns provider identifier name "KIMI".
     * @return "KIMI" string.
     */
    @Override
    public String getProviderName() {
        return "KIMI";
    }

    /**
     * @brief Sends chat completion request to Moonshot AI Kimi API using moonshot-v1-8k.
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
            "model", "moonshot-v1-8k",
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

        throw new RuntimeException("Kimi API returned status: " + response.getStatusCode());
    }
}
