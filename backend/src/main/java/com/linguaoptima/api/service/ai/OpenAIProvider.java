/**
 * @file OpenAIProvider.java
 * @brief OpenAI API provider integration supporting BYOK user keys and gpt-4o-mini inference.
 */
package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @brief OpenAI API provider integration supporting BYOK user keys and gpt-4o-mini inference.
 */
@Component
public class OpenAIProvider implements AIProvider {

    /** @brief Field representing api key in OpenAIProvider. */
    private final String apiKey;
    /** @brief Field representing rest template in OpenAIProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in OpenAIProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs an OpenAIProvider with default dummy credentials.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public OpenAIProvider(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this("dummy-openai-key", restTemplate, objectMapper);
    }

    /**
     * @brief Constructs an OpenAIProvider with explicit API key.
     * @param apiKey OpenAI API secret key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    public OpenAIProvider(String apiKey, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
     * @brief Sends chat completion request to OpenAI API using gpt-4o-mini.
     * @param prompt Input prompt text.
     * @return Model response text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "model", "gpt-4o-mini",
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
