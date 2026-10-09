/**
 * @file GeminiProvider.java
 * @brief Google Gemini API provider utilizing gemini-1.5-flash for essay evaluations and fallback generation.
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

/**
 * @brief Google Gemini API provider utilizing gemini-1.5-flash for essay evaluations and fallback generation.
 */
@Slf4j
@Component
public class GeminiProvider implements AIProvider {

    /** @brief Field representing api key in GeminiProvider. */
    private final String apiKey;
    /** @brief Field representing rest template in GeminiProvider. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in GeminiProvider. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs a GeminiProvider with injected API key and HTTP client components.
     * @param apiKey Configured Gemini API key.
     * @param restTemplate RestTemplate HTTP client.
     * @param objectMapper Jackson JSON mapper.
     */
    @Autowired
    public GeminiProvider(
        @Value("${app.ai.gemini.api-key:dummy-gemini-key}") String apiKey,
        RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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
     * @brief Sends content generation request to Google Gemini API using gemini-1.5-flash.
     * @param prompt Input prompt text.
     * @return Generated model candidate text.
     * @throws Exception if HTTP exchange fails or non-2xx status code is returned.
     */
    @Override
    public String complete(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

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

        throw new RuntimeException("Gemini API returned error status: " + response.getStatusCode());
    }
}
