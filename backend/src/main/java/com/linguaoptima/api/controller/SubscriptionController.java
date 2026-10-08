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

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UsageService usageService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionResponse> getMySubscription(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.getSubscription(user));
    }

    @PostMapping("/upgrade")
    public ResponseEntity<PaymentResultResponse> upgrade(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UpgradeRequest request
    ) {
        return ResponseEntity.ok(subscriptionService.upgrade(user, request.getTargetTier(), request.getPaymentToken()));
    }

    @PostMapping("/downgrade")
    public ResponseEntity<Void> downgrade(@AuthenticationPrincipal User user) {
        subscriptionService.downgrade(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usage")
    public ResponseEntity<UsageResponse> getUsage(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(usageService.getUsage(user));
    }
}
