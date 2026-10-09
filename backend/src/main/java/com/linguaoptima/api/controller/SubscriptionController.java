/**
 * @file SubscriptionController.java
 * @brief REST controller for managing subscription tiers and tracking usage quotas.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.UpgradeRequest;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.dto.response.SubscriptionResponse;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.service.SubscriptionService;
import com.linguaoptima.api.service.UsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @brief REST controller for managing subscription tiers and tracking usage quotas.
 *
 * Coordinates Free, Premium, and Educator tier upgrades via mock payment processing.
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    /** @brief Field representing subscription service in SubscriptionController. */
    private final SubscriptionService subscriptionService;
    /** @brief Field representing usage service in SubscriptionController. */
    private final UsageService usageService;

    /**
     * @brief Retrieves active subscription plan details for authenticated user.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with SubscriptionResponse.
     */
    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.getSubscription(user));
    }

    /**
     * @brief Upgrades user subscription plan via payment provider stub.
     *
     * @param user Authenticated user principal.
     * @param request Payload containing target subscription tier and payment token.
     * @return HTTP 200 with PaymentResultResponse.
     */
    @PostMapping("/upgrade")
    public ResponseEntity<PaymentResultResponse> upgrade(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpgradeRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.upgrade(user, request.getTargetTier(), request.getPaymentToken()));
    }

    /**
     * @brief Downgrades user subscription to the Free learner plan.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 OK.
     */
    @PostMapping("/downgrade")
    public ResponseEntity<Void> downgrade(@AuthenticationPrincipal User user) {
        subscriptionService.downgrade(user);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Fetches remaining daily and weekly quota counters for AI and OCR calls.
     *
     * @param user Authenticated user principal.
     * @return HTTP 200 with UsageResponse detailing consumed and remaining quotas.
     */
    @GetMapping("/usage")
    public ResponseEntity<UsageResponse> getUsage(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(usageService.getUsage(user));
    }
}
