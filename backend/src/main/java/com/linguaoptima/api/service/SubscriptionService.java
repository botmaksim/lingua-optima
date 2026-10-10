/**
 * @file SubscriptionService.java
 * @brief Subscription lifecycle and access tier enforcement service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.dto.response.SubscriptionResponse;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @brief Subscription lifecycle and access tier enforcement service.
 *
 * Enforces tier restrictions across FREE, PREMIUM, and EDUCATOR tiers, including
 * C1 CEFR content gating, feature permissions, and payment upgrades.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    /** @brief Field representing subscription repository in SubscriptionService. */
    private final SubscriptionRepository subscriptionRepository;
    /** @brief Field representing payment service in SubscriptionService. */
    private final PaymentService paymentService;
    /** @brief Field representing usage service in SubscriptionService. */
    private final UsageService usageService;

    /**
     * @brief Retrieves active user subscription or initializes a FREE tier subscription if none exists.
     * @param user User whose subscription is requested.
     * @return Existing or newly created Subscription entity.
     */
    @Transactional
    public Subscription getOrCreateSubscription(User user) {
        return subscriptionRepository.findByUserId(user.getId())
            .orElseGet(() -> subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.FREE)
                .createdAt(LocalDateTime.now())
                .build()));
    }

    /**
     * @brief Computes subscription details and remaining quotas for the specified user.
     * @param user Authenticated user querying their subscription.
     * @return SubscriptionResponse DTO containing tier, expiration date, and quota remaining.
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(User user) {
        Subscription subscription = getOrCreateSubscription(user);
        UsageResponse usage = usageService.getUsage(user);

        return SubscriptionResponse.builder()
            .tier(subscription.getTier())
            .expiresAt(subscription.getExpiresAt())
            .evaluationsRemaining(usage.getEvaluationsRemaining())
            .ocrRemaining(usage.getOcrRemaining())
            .unlimited(false)
            .build();
    }

    /**
     * @brief Upgrades user subscription tier upon successful payment verification.
     * @param user Authenticated user requesting the upgrade.
     * @param targetTier Desired subscription tier (PREMIUM or EDUCATOR).
     * @param paymentToken Payment processing token.
     * @return PaymentResultResponse DTO containing transaction status and identifiers.
     */
    @Transactional
    public PaymentResultResponse upgrade(User user, SubscriptionTier targetTier, String paymentToken) {
        if (targetTier == SubscriptionTier.FREE) {
            downgrade(user);
            return PaymentResultResponse.builder()
                .success(true)
                .transactionId("DOWNGRADE-FREE")
                .build();
        }

        double amount = (targetTier == SubscriptionTier.PREMIUM) ? 9.99 : 29.99;
        PaymentResultResponse paymentResult = paymentService.processPayment(amount, paymentToken);

        if (paymentResult.isSuccess()) {
            Subscription subscription = getOrCreateSubscription(user);
            subscription.setTier(targetTier);
            subscription.setExpiresAt(LocalDateTime.now().plusDays(30));
            subscriptionRepository.save(subscription);
            log.info("User {} upgraded to tier {}", user.getEmail(), targetTier);
        }

        return paymentResult;
    }

    /**
     * @brief Downgrades user subscription to the FREE tier and removes expiry restrictions.
     * @param user User requesting downgrade.
     */
    @Transactional
    public void downgrade(User user) {
        Subscription subscription = getOrCreateSubscription(user);
        subscription.setTier(SubscriptionTier.FREE);
        subscription.setExpiresAt(null);
        subscriptionRepository.save(subscription);
        log.info("User {} downgraded to FREE tier", user.getEmail());
    }

    /**
     * @brief Verifies whether user tier grants access to requested CEFR level (e.g. C1/C2 requires paid tier).
     * @param user Authenticated user requesting task content.
     * @param requestedLevel Requested CEFR proficiency level.
     * @throws ForbiddenException if C1 or C2 content is requested by a FREE tier user.
     */
    public void validateCefrLevelAccess(User user, CefrLevel requestedLevel) {
        Subscription subscription = getOrCreateSubscription(user);
        if ((requestedLevel == CefrLevel.C1 || requestedLevel == CefrLevel.C2) && subscription.getTier() == SubscriptionTier.FREE) {
            throw new ForbiddenException("C1 and C2 CEFR levels require PREMIUM or EDUCATOR tier.");
        }
    }

    /**
     * @brief Evaluates whether a designated platform feature is permitted for user's active tier.
     * @param user Authenticated user.
     * @param feature Feature identifier name (DEPLOY, OVERRIDE, EXPORT, API_KEYS, UNLIMITED_EVALS, UNLIMITED_OCR, C1_LEVEL, C2_LEVEL).
     * @return True if permitted, false otherwise.
     */
    public boolean isFeatureAllowed(User user, String feature) {
        Subscription subscription = getOrCreateSubscription(user);
        SubscriptionTier tier = subscription.getTier();

        return switch (feature.toUpperCase()) {
            case "DEPLOY", "OVERRIDE", "EXPORT", "API_KEYS" -> tier == SubscriptionTier.EDUCATOR;
            case "UNLIMITED_EVALS", "UNLIMITED_OCR", "C1_LEVEL", "C2_LEVEL" -> tier != SubscriptionTier.FREE;
            default -> true;
        };
    }
}
