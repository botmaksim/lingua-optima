package com.linguaoptima.api.service;

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
 * @file UsageService.java
 * @brief Quota tracking and enforcement service for free-tier users.
 *
 * Enforces weekly caps (10 evaluations, 3 OCR uploads) on FREE tier accounts,
 * permitting unlimited evaluations and uploads for PREMIUM and EDUCATOR subscribers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageService {

    /**
     * @brief Weekly evaluation limit for FREE tier users.
     */
    public static final int FREE_EVAL_LIMIT = 10;

    /**
     * @brief Weekly OCR upload limit for FREE tier users.
     */
    public static final int FREE_OCR_LIMIT = 3;

    private final UsageCounterRepository usageCounterRepository;
    private final SubscriptionRepository subscriptionRepository;

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
     * @brief Increments evaluation counter for free-tier users, enforcing weekly cap.
     * @param user Authenticated user initiating evaluation.
     * @throws QuotaExceededException if free user has exhausted weekly evaluation limit.
     */
    @Transactional
    public void incrementEvaluation(User user) {
        SubscriptionTier tier = getUserTier(user);
        if (tier != SubscriptionTier.FREE) {
            return;
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekEvaluations() >= FREE_EVAL_LIMIT) {
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
        if (tier != SubscriptionTier.FREE) {
            return;
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekOcrUploads() >= FREE_OCR_LIMIT) {
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
        UsageCounter counter = usageCounterRepository.findByUserId(user.getId())
            .orElse(UsageCounter.builder()
                .user(user)
                .weekEvaluations(0)
                .weekOcrUploads(0)
                .weekResetAt(LocalDateTime.now())
                .build());

        if (tier == SubscriptionTier.FREE) {
            int evalsUsed = counter.getWeekEvaluations();
            int ocrUsed = counter.getWeekOcrUploads();
            return UsageResponse.builder()
                .weekEvaluations(evalsUsed)
                .weekOcrUploads(ocrUsed)
                .evaluationLimit(FREE_EVAL_LIMIT)
                .ocrLimit(FREE_OCR_LIMIT)
                .evaluationsRemaining(Math.max(0, FREE_EVAL_LIMIT - evalsUsed))
                .ocrRemaining(Math.max(0, FREE_OCR_LIMIT - ocrUsed))
                .weekResetAt(counter.getWeekResetAt())
                .build();
        } else {
            return UsageResponse.builder()
                .weekEvaluations(counter.getWeekEvaluations())
                .weekOcrUploads(counter.getWeekOcrUploads())
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
            counter.setWeekResetAt(LocalDateTime.now());
            usageCounterRepository.save(counter);
        });
    }
}
