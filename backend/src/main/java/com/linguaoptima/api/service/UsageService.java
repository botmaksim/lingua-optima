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

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageService {

    public static final int FREE_EVAL_LIMIT = 10;
    public static final int FREE_OCR_LIMIT = 3;

    private final UsageCounterRepository usageCounterRepository;
    private final SubscriptionRepository subscriptionRepository;

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

    public SubscriptionTier getUserTier(User user) {
        return subscriptionRepository.findByUserId(user.getId())
            .map(Subscription::getTier)
            .orElse(SubscriptionTier.FREE);
    }

    @Transactional
    public void incrementEvaluation(User user) {
        SubscriptionTier tier = getUserTier(user);
        if (tier != SubscriptionTier.FREE) {
            return; // Unlimited for PREMIUM and EDUCATOR
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekEvaluations() >= FREE_EVAL_LIMIT) {
            throw new QuotaExceededException("Weekly evaluation limit reached. Please upgrade to continue.");
        }
        counter.setWeekEvaluations(counter.getWeekEvaluations() + 1);
        usageCounterRepository.save(counter);
    }

    @Transactional
    public void incrementOcr(User user) {
        SubscriptionTier tier = getUserTier(user);
        if (tier != SubscriptionTier.FREE) {
            return; // Unlimited
        }

        UsageCounter counter = getOrCreateCounter(user);
        if (counter.getWeekOcrUploads() >= FREE_OCR_LIMIT) {
            throw new QuotaExceededException("Weekly OCR upload limit reached. Please upgrade to continue.");
        }
        counter.setWeekOcrUploads(counter.getWeekOcrUploads() + 1);
        usageCounterRepository.save(counter);
    }

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
