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

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentService paymentService;
    private final UsageService usageService;

    @Transactional
    public Subscription getOrCreateSubscription(User user) {
        return subscriptionRepository.findByUserId(user.getId())
            .orElseGet(() -> subscriptionRepository.save(Subscription.builder()
                .user(user)
                .tier(SubscriptionTier.FREE)
                .createdAt(LocalDateTime.now())
                .build()));
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(User user) {
        Subscription subscription = getOrCreateSubscription(user);
        UsageResponse usage = usageService.getUsage(user);

        return SubscriptionResponse.builder()
            .tier(subscription.getTier())
            .expiresAt(subscription.getExpiresAt())
            .evaluationsRemaining(usage.getEvaluationsRemaining())
            .ocrRemaining(usage.getOcrRemaining())
            .unlimited(subscription.getTier() != SubscriptionTier.FREE)
            .build();
    }

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

    @Transactional
    public void downgrade(User user) {
        Subscription subscription = getOrCreateSubscription(user);
        subscription.setTier(SubscriptionTier.FREE);
        subscription.setExpiresAt(null);
        subscriptionRepository.save(subscription);
        log.info("User {} downgraded to FREE tier", user.getEmail());
    }

    public void validateCefrLevelAccess(User user, CefrLevel requestedLevel) {
        Subscription subscription = getOrCreateSubscription(user);
        if (requestedLevel == CefrLevel.C1 && subscription.getTier() == SubscriptionTier.FREE) {
            throw new ForbiddenException("C1 CEFR level requires PREMIUM or EDUCATOR tier.");
        }
    }

    public boolean isFeatureAllowed(User user, String feature) {
        Subscription subscription = getOrCreateSubscription(user);
        SubscriptionTier tier = subscription.getTier();

        return switch (feature.toUpperCase()) {
            case "DEPLOY", "OVERRIDE", "EXPORT", "API_KEYS" -> tier == SubscriptionTier.EDUCATOR;
            case "UNLIMITED_EVALS", "UNLIMITED_OCR", "C1_LEVEL" -> tier != SubscriptionTier.FREE;
            default -> true;
        };
    }
}
