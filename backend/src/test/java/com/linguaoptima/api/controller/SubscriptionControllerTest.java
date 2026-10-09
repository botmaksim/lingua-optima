/**
 * @file SubscriptionControllerTest.java
 * @brief Unit and slice test suite for SubscriptionController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.UpgradeRequest;
import com.linguaoptima.api.dto.response.PaymentResultResponse;
import com.linguaoptima.api.dto.response.SubscriptionResponse;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.service.SubscriptionService;
import com.linguaoptima.api.service.UsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for SubscriptionController.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    /** @brief Test fixture or mock dependency for subscription service. */
    @Mock
    private SubscriptionService subscriptionService;

    /** @brief Test fixture or mock dependency for usage service. */
    @Mock
    private UsageService usageService;

    /** @brief Test fixture or mock dependency for subscription controller. */
    @InjectMocks
    private SubscriptionController subscriptionController;

    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in SubscriptionControllerTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
    }

    /**
     * @brief Verifies unit test scenario: get subscription and usage.
     */
    @Test
    void testGetSubscriptionAndUsage() {
        SubscriptionResponse subRes = SubscriptionResponse.builder().tier(SubscriptionTier.FREE).build();
        when(subscriptionService.getSubscription(user)).thenReturn(subRes);
        assertEquals(HttpStatus.OK, subscriptionController.getMySubscription(user).getStatusCode());

        UsageResponse usageRes = UsageResponse.builder().weekEvaluations(2).build();
        when(usageService.getUsage(user)).thenReturn(usageRes);
        assertEquals(HttpStatus.OK, subscriptionController.getUsage(user).getStatusCode());
    }

    /**
     * @brief Verifies unit test scenario: upgrade and downgrade.
     */
    @Test
    void testUpgradeAndDowngrade() {
        UpgradeRequest req = UpgradeRequest.builder().targetTier(SubscriptionTier.PREMIUM).paymentToken("tok").build();
        PaymentResultResponse payRes = PaymentResultResponse.builder().success(true).build();
        when(subscriptionService.upgrade(user, SubscriptionTier.PREMIUM, "tok")).thenReturn(payRes);

        assertEquals(HttpStatus.OK, subscriptionController.upgrade(user, req).getStatusCode());

        ResponseEntity<Void> downRes = subscriptionController.downgrade(user);
        assertEquals(HttpStatus.OK, downRes.getStatusCode());
        verify(subscriptionService).downgrade(user);
    }
}
