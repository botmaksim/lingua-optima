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

@Slf4j
@Service
public class AIBrokerService {

    private final GroqProvider groqProvider;
    private final GeminiProvider geminiProvider;
    private final ApiKeyRepository apiKeyRepository;
    private final EncryptionService encryptionService;
    private final PendingAiTaskRepository pendingAiTaskRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

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

    public String scoreEssay(String prompt, User user) {
        checkUserRateLimit(user);
        // NO CACHING: Essay scoring is strictly unique per essay
        AIProvider customProvider = getUserCustomProvider(user).orElse(null);
        if (customProvider != null) {
            return executeWithUserKey(customProvider, prompt);
        }
        return executeWithFallback(geminiProvider, groqProvider, prompt, user, "ESSAY_SCORING");
    }

    public String checkGrammar(String prompt, User user) {
        checkUserRateLimit(user);
        AIProvider customProvider = getUserCustomProvider(user).orElse(null);
        if (customProvider != null) {
            return executeWithUserKey(customProvider, prompt);
        }
        return executeWithFallback(groqProvider, geminiProvider, prompt, user, "GRAMMAR_CHECK");
    }

    private String executeWithUserKey(AIProvider provider, String prompt) {
        try {
            return provider.complete(prompt);
        } catch (Exception e) {
            log.error("Failed AI call with user custom key: {}", e.getMessage());
            throw new PaymentException("Custom API key failed: " + e.getMessage() + ". Please check your key.");
        }
    }

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
        };
    }

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
            if (count != null && count > 500L) { // Daily burst ceiling
                throw new QuotaExceededException("Daily AI request limit reached. Please try again later.");
            }
        } catch (QuotaExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rate limit check failed, proceeding: {}", e.getMessage());
        }
    }

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
            return Integer.toHexString(text.hashCode());
        }
    }
}
