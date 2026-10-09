/**
 * @file AIBrokerService.java
 * @brief Multi-provider AI broker and fallback orchestration service.
 */
package com.linguaoptima.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.ApiKey;
import com.linguaoptima.api.domain.PendingAiTask;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.exception.AIServiceException;
import com.linguaoptima.api.exception.PaymentException;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.repository.ApiKeyRepository;
import com.linguaoptima.api.repository.PendingAiTaskRepository;
import com.linguaoptima.api.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * @brief Multi-provider AI broker and fallback orchestration service.
 *
 * Implements intelligent model routing between primary (Groq/Gemini) and fallback models,
 * custom BYOK user key routing, response caching via Redis, and asynchronous retry queueing.
 */
@Slf4j
@Service
public class AIBrokerService {

    /** @brief Field representing groq provider in AIBrokerService. */
    private final GroqProvider groqProvider;
    /** @brief Field representing gemini provider in AIBrokerService. */
    private final GeminiProvider geminiProvider;
    /** @brief Field representing api key repository in AIBrokerService. */
    private final ApiKeyRepository apiKeyRepository;
    /** @brief Field representing encryption service in AIBrokerService. */
    private final EncryptionService encryptionService;
    /** @brief Field representing pending ai task repository in AIBrokerService. */
    private final PendingAiTaskRepository pendingAiTaskRepository;
    /** @brief Field representing rest template in AIBrokerService. */
    private final RestTemplate restTemplate;
    /** @brief Field representing object mapper in AIBrokerService. */
    private final ObjectMapper objectMapper;
    /** @brief Field representing string redis template in AIBrokerService. */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * @brief Constructs an AIBrokerService with injected AI providers and repositories.
     * @param groqProvider Default fast Groq provider.
     * @param geminiProvider Multimodal Gemini provider.
     * @param apiKeyRepository BYOK key repository.
     * @param encryptionService Decryption service for BYOK credentials.
     * @param pendingAiTaskRepository Repository for queuing failed inference tasks.
     * @param restTemplate HTTP client for dynamic provider instantiation.
     * @param objectMapper Jackson JSON mapper.
     * @param stringRedisTemplate Optional Redis template for response caching.
     */
    @Autowired
    public AIBrokerService(
        GroqProvider groqProvider,
        GeminiProvider geminiProvider,
        ApiKeyRepository apiKeyRepository,
        EncryptionService encryptionService,
        PendingAiTaskRepository pendingAiTaskRepository,
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Autowired(required = false) StringRedisTemplate stringRedisTemplate
    ) {
        this.groqProvider = groqProvider;
        this.geminiProvider = geminiProvider;
        this.apiKeyRepository = apiKeyRepository;
        this.encryptionService = encryptionService;
        this.pendingAiTaskRepository = pendingAiTaskRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * @brief Generates task content via AI with 1-hour Redis caching and fallback protection.
     * @param prompt Task generation prompt.
     * @param user User initiating the generation.
     * @return Raw JSON response containing generated questions and metadata.
     */
    public String generateTaskContent(String prompt, User user) {
        checkUserRateLimit(user);

        String cacheKey = "ai_cache:" + sha256(prompt);
        if (stringRedisTemplate != null) {
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isBlank()) {
                    log.info("Task generation cache hit for hash: {}", sha256(prompt));
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed: {}", e.getMessage());
            }
        }

        AIProvider customProvider = getUserCustomProvider(user).orElse(null);
        String result;
        if (customProvider != null) {
            result = executeWithUserKey(customProvider, prompt);
        } else {
            result = executeWithFallback(groqProvider, geminiProvider, prompt, user, "TASK_GENERATION");
        }

        if (stringRedisTemplate != null && result != null) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, result, Duration.ofHours(1));
            } catch (Exception e) {
                log.warn("Redis cache write failed: {}", e.getMessage());
            }
        }

        return result;
    }

    /**
     * @brief Evaluates and grades student essay using Gemini (fallback Groq), without caching.
     * @param prompt Essay evaluation rubric prompt.
     * @param user Authenticated student.
     * @return AI evaluation JSON string.
     */
    public String scoreEssay(String prompt, User user) {
        checkUserRateLimit(user);
        AIProvider customProvider = getUserCustomProvider(user).orElse(null);
        if (customProvider != null) {
            return executeWithUserKey(customProvider, prompt);
        }
        return executeWithFallback(geminiProvider, groqProvider, prompt, user, "ESSAY_SCORING");
    }

    /**
     * @brief Checks and scores grammar exercises using Groq (fallback Gemini).
     * @param prompt Grammar evaluation prompt.
     * @param user Authenticated student.
     * @return AI evaluation JSON string.
     */
    public String checkGrammar(String prompt, User user) {
        checkUserRateLimit(user);
        AIProvider customProvider = getUserCustomProvider(user).orElse(null);
        if (customProvider != null) {
            return executeWithUserKey(customProvider, prompt);
        }
        return executeWithFallback(groqProvider, geminiProvider, prompt, user, "GRAMMAR_CHECK");
    }

    /**
     * @brief Executes inference using a user's decrypted custom BYOK key.
     * @param provider Configured AI provider instance.
     * @param prompt Prompt string.
     * @return AI response string.
     * @throws PaymentException if user's API key is rejected by the third-party provider.
     */
    private String executeWithUserKey(AIProvider provider, String prompt) {
        try {
            return provider.complete(prompt);
        } catch (Exception e) {
            log.error("Failed AI call with user custom key: {}", e.getMessage());
            throw new PaymentException("Custom API key failed: " + e.getMessage() + ". Please check your key.");
        }
    }

    /**
     * @brief Executes inference with automatic fallback between primary and secondary providers.
     * @param primary Preferred primary provider.
     * @param secondary Fallback secondary provider.
     * @param prompt Input prompt.
     * @param user User initiating the request.
     * @param taskType Task category identifier for queue tracking.
     * @return AI response string.
     * @throws AIServiceException if all providers fail, after enqueuing for background processing.
     */
    private String executeWithFallback(AIProvider primary, AIProvider secondary, String prompt, User user, String taskType) {
        try {
            log.info("Attempting primary AI provider: {}", primary.getProviderName());
            return primary.complete(prompt);
        } catch (Exception e1) {
            log.warn("Primary provider {} failed: {}. Falling back to secondary: {}",
                primary.getProviderName(), e1.getMessage(), secondary.getProviderName());
            try {
                return secondary.complete(prompt);
            } catch (Exception e2) {
                log.warn("Secondary provider {} failed: {}. Retrying primary...", secondary.getProviderName(), e2.getMessage());
                try {
                    return primary.complete(prompt);
                } catch (Exception e3) {
                    log.error("All AI providers failed. Queueing request.");
                    if (user != null && user.getId() != null) {
                        pendingAiTaskRepository.save(PendingAiTask.builder()
                            .userId(user.getId())
                            .taskType(taskType)
                            .prompt(prompt)
                            .status("QUEUED")
                            .build());
                    }
                    throw new AIServiceException("AI service temporarily unavailable. Your request has been queued.");
                }
            }
        }
    }

    /**
     * @brief Resolves custom BYOK provider configured for the user if available.
     * @param user User requesting AI generation.
     * @return Optional containing AIProvider, or empty if standard system keys are to be used.
     */
    private Optional<AIProvider> getUserCustomProvider(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        List<ApiKey> keys = apiKeyRepository.findAllByUserId(user.getId());
        if (keys.isEmpty()) {
            return Optional.empty();
        }

        ApiKey key = keys.get(0);
        String decryptedKey = encryptionService.decrypt(key.getEncryptedKey());

        return switch (key.getProvider()) {
            case GROQ -> Optional.of((AIProvider) new GroqProvider(decryptedKey, restTemplate, objectMapper));
            case GEMINI -> Optional.of((AIProvider) new GeminiProvider(decryptedKey, restTemplate, objectMapper));
            case OPENAI -> Optional.of((AIProvider) new OpenAIProvider(decryptedKey, restTemplate, objectMapper));
            case ANTHROPIC -> Optional.of((AIProvider) new AnthropicProvider(decryptedKey, restTemplate, objectMapper));
            case DEEPSEEK -> Optional.of((AIProvider) new DeepSeekProvider(decryptedKey, restTemplate, objectMapper));
            case QWEN -> Optional.of((AIProvider) new QwenProvider(decryptedKey, restTemplate, objectMapper));
            case KIMI -> Optional.of((AIProvider) new KimiProvider(decryptedKey, restTemplate, objectMapper));
        };
    }

    /**
     * @brief Checks daily burst limits on AI requests per user in Redis (max 500 requests/day).
     * @param user Authenticated user.
     * @throws QuotaExceededException if daily request ceiling is breached.
     */
    private void checkUserRateLimit(User user) {
        if (user == null || user.getId() == null || stringRedisTemplate == null) {
            return;
        }
        try {
            String rateLimitKey = "rate_limit:" + user.getId() + ":ai";
            Long count = stringRedisTemplate.opsForValue().increment(rateLimitKey);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(rateLimitKey, Duration.ofHours(24));
            }
            if (count != null && count > 500L) {
                throw new QuotaExceededException("Daily AI request limit reached. Please try again later.");
            }
        } catch (QuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rate limit check failed, proceeding: {}", e.getMessage());
        }
    }

    /**
     * @brief Computes SHA-256 hash string for prompt caching.
     * @param text Prompt string to hash.
     * @return Hexadecimal SHA-256 string.
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toHexString(java.util.Objects.hashCode(text));
        }
    }
}
