/**
 * @file UsageService.java
 * @brief Quota tracking and enforcement service for free-tier users.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.config.PricingProperties;
import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.UsageCounter;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UsageCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @brief Quota tracking and enforcement service for free-tier and premium users.
 *
 * Enforces weekly caps on tokens, evaluations, and OCR uploads based on configurable
 * properties in PricingProperties, permitting scalable limits across FREE, PREMIUM, and EDUCATOR tiers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageService {

    /** @brief Field representing usage counter repository in UsageService. */
    private final UsageCounterRepository usageCounterRepository;
    /** @brief Field representing subscription repository in UsageService. */
    private final SubscriptionRepository subscriptionRepository;
    /** @brief Configurable pricing and quota rules externalized from code. */
    private final PricingProperties pricingProperties;

    /**
     * @brief Retrieves existing usage counter or initializes a new one.
     * @param user User whose counter is requested.
     * @return UsageCounter entity.
     */
    @Transactional
    public UsageCounter getOrCreateCounter(User user) {
        return usageCounterRepository.findByUserId(user.getId())
            .orElseGet(() -> usageCounterRepository.save(UsageCounter.builder()
                .user(user)
                .weekEvaluations(0)
                .weekOcrUploads(0)
                .weekTokensUsed(0L)
                .weekResetAt(LocalDateTime.now())
                .build()));
    }

    /**
     * @brief Resolves active subscription tier for a user.
     * @param user User whose tier is queried.
     * @return SubscriptionTier enum value.
     */
    public SubscriptionTier getUserTier(User user) {
        return subscriptionRepository.findByUserId(user.getId())
            .map(Subscription::getTier)
            .orElse(SubscriptionTier.FREE);
    }

    /**
     * @brief Consumes a specified number of tokens, enforcing weekly token quota for the user tier.
     * @param user Authenticated user initiating inference.
     * @param tokens Number of tokens consumed.
     * @throws QuotaExceededException if user's weekly token quota is breached.
     */
    @Transactional
    public void consumeTokens(User user, long tokens) {
        if (tokens <= 0) return;
        SubscriptionTier tier = getUserTier(user);
        PricingProperties.TierConfig config = pricingProperties.getTierConfig(tier);

        UsageCounter counter = getOrCreateCounter(user);
        if (tier == SubscriptionTier.FREE && (counter.getWeekTokensUsed() + tokens > config.getWeeklyTokenLimit())) {
            throw new QuotaExceededException(
                "Weekly AI token allowance of " + config.getWeeklyTokenLimit() + " tokens exceeded. Upgrade to continue."
            );
        }
        counter.setWeekTokensUsed(counter.getWeekTokensUsed() + tokens);
        usageCounterRepository.save(counter);
    }

    /**
     * @brief Estimates token count from text using standard ~4 characters per token heuristic.
     * @param text Input or output text.
     * @return Estimated token count (minimum 1).
     */
    public static long estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0L;
        return Math.max(1L, text.length() / 4L);
    }

    /**
     * @brief Increments evaluation counter for free-tier users, enforcing weekly cap.
     * @param user Authenticated user initiating evaluation.
     * @throws QuotaExceededException if free user has exhausted weekly evaluation limit.
     */
    @Transactional
    public void incrementEvaluation(User user) {
        SubscriptionTier tier = getUserTier(user);
        PricingProperties.TierConfig config = pricingProperties.getTierConfig(tier);

        if (tier != SubscriptionTier.FREE) {
            return;
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekEvaluations() >= config.getWeeklyEvaluationLimit()) {
            throw new QuotaExceededException("Weekly evaluation limit reached. Please upgrade to continue.");
        }
        counter.setWeekEvaluations(counter.getWeekEvaluations() + 1);
        usageCounterRepository.save(counter);
    }

    /**
     * @brief Increments OCR upload counter for free-tier users, enforcing weekly cap.
     * @param user Authenticated user initiating OCR upload.
     * @throws QuotaExceededException if free user has exhausted weekly OCR upload limit.
     */
    @Transactional
    public void incrementOcr(User user) {
        SubscriptionTier tier = getUserTier(user);
        PricingProperties.TierConfig config = pricingProperties.getTierConfig(tier);

        if (tier != SubscriptionTier.FREE) {
            return;
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekOcrUploads() >= config.getWeeklyOcrLimit()) {
            throw new QuotaExceededException("Weekly OCR upload limit reached. Please upgrade to continue.");
        }
        counter.setWeekOcrUploads(counter.getWeekOcrUploads() + 1);
        usageCounterRepository.save(counter);
    }

    /**
     * @brief Retrieves current weekly usage metrics and remaining quotas.
     * @param user User querying usage.
     * @return UsageResponse DTO containing usage statistics and quota limits.
     */
    @Transactional(readOnly = true)
    public UsageResponse getUsage(User user) {
        SubscriptionTier tier = getUserTier(user);
        PricingProperties.TierConfig config = pricingProperties.getTierConfig(tier);

        UsageCounter counter = usageCounterRepository.findByUserId(user.getId())
            .orElse(UsageCounter.builder()
                .user(user)
                .weekEvaluations(0)
                .weekOcrUploads(0)
                .weekTokensUsed(0L)
                .weekResetAt(LocalDateTime.now())
                .build());

        int evalsUsed = counter.getWeekEvaluations();
        int ocrUsed = counter.getWeekOcrUploads();
        long tokensUsed = counter.getWeekTokensUsed();

        if (tier == SubscriptionTier.FREE) {
            int evalLimit = config.getWeeklyEvaluationLimit();
            int ocrLimit = config.getWeeklyOcrLimit();
            long tokenLimit = config.getWeeklyTokenLimit();

            return UsageResponse.builder()
                .weekEvaluations(evalsUsed)
                .weekOcrUploads(ocrUsed)
                .weekTokensUsed(tokensUsed)
                .tokenLimit(tokenLimit)
                .tokensRemaining(Math.max(0L, tokenLimit - tokensUsed))
                .evaluationLimit(evalLimit)
                .ocrLimit(ocrLimit)
                .evaluationsRemaining(Math.max(0, evalLimit - evalsUsed))
                .ocrRemaining(Math.max(0, ocrLimit - ocrUsed))
                .weekResetAt(counter.getWeekResetAt())
                .build();
        } else {
            long tokenLimit = config.getWeeklyTokenLimit();
            return UsageResponse.builder()
                .weekEvaluations(evalsUsed)
                .weekOcrUploads(ocrUsed)
                .weekTokensUsed(tokensUsed)
                .tokenLimit(tokenLimit)
                .tokensRemaining(Math.max(0L, tokenLimit - tokensUsed))
                .evaluationLimit(null)
                .ocrLimit(null)
                .evaluationsRemaining(Integer.MAX_VALUE)
                .ocrRemaining(Integer.MAX_VALUE)
                .weekResetAt(counter.getWeekResetAt())
                .build();
        }
    }

    /**
     * @brief Resets all weekly counters across all users to zero.
     */
    @Transactional
    public void resetWeeklyCounters() {
        log.info("Resetting all weekly usage counters for all users");
        usageCounterRepository.findAll().forEach(counter -> {
            counter.setWeekEvaluations(0);
            counter.setWeekOcrUploads(0);
            counter.setWeekTokensUsed(0L);
            counter.setWeekResetAt(LocalDateTime.now());
            usageCounterRepository.save(counter);
        });
    }
}
