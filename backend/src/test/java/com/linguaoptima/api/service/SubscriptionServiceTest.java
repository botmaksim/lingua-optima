/**
 * @file SubscriptionServiceTest.java
 * @brief Unit and slice test suite for SubscriptionService.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for SubscriptionService.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    /** @brief Test fixture or mock dependency for subscription repository. */
    @Mock
    private SubscriptionRepository subscriptionRepository;
    /** @brief Test fixture or mock dependency for payment service. */
    @Mock
    private PaymentService paymentService;
    /** @brief Test fixture or mock dependency for usage service. */
    @Mock
    private UsageService usageService;

    /** @brief Test fixture or mock dependency for subscription service. */
    @InjectMocks
    private SubscriptionService subscriptionService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;
    /** @brief Test fixture or mock dependency for subscription. */
    private Subscription subscription;

    /**
     * @brief Initializes test fixtures and mock state before each test in SubscriptionServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("sub@lingua.com").build();
        subscription = Subscription.builder().id(UUID.randomUUID()).user(user).tier(SubscriptionTier.FREE).build();
    }

    /**
     * @brief Verifies unit test scenario: get subscription.
     */
    @Test
    void testGetSubscription() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(usageService.getUsage(user)).thenReturn(UsageResponse.builder()
            .evaluationsRemaining(10)
            .ocrRemaining(3)
            .build());

        SubscriptionResponse res = subscriptionService.getSubscription(user);
        assertNotNull(res);
        assertEquals(SubscriptionTier.FREE, res.getTier());
        assertEquals(10, res.getEvaluationsRemaining());
        assertFalse(res.isUnlimited());
    }

    /**
     * @brief Verifies unit test scenario: upgrade to premium success.
     */
    @Test
    void testUpgradeToPremiumSuccess() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(paymentService.processPayment(eq(9.99), eq("token123"))).thenReturn(
            PaymentResultResponse.builder().success(true).transactionId("STUB-123").build());
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResultResponse res = subscriptionService.upgrade(user, SubscriptionTier.PREMIUM, "token123");
        assertTrue(res.isSuccess());
        assertEquals(SubscriptionTier.PREMIUM, subscription.getTier());
        assertNotNull(subscription.getExpiresAt());
    }

    /**
     * @brief Verifies unit test scenario: upgrade to free downgrades.
     */
    @Test
    void testUpgradeToFreeDowngrades() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResultResponse res = subscriptionService.upgrade(user, SubscriptionTier.FREE, null);
        assertTrue(res.isSuccess());
        assertEquals(SubscriptionTier.FREE, subscription.getTier());
    }

    /**
     * @brief Verifies unit test scenario: downgrade.
     */
    @Test
    void testDowngrade() {
        subscription.setTier(SubscriptionTier.PREMIUM);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.downgrade(user);
        assertEquals(SubscriptionTier.FREE, subscription.getTier());
        assertNull(subscription.getExpiresAt());
    }

    /**
     * @brief Verifies unit test scenario: validate cefr level access.
     */
    @Test
    void testValidateCefrLevelAccess() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));

        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.B1));
        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.B2));

        assertThrows(ForbiddenException.class, () -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.C1));

        subscription.setTier(SubscriptionTier.PREMIUM);
        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.C1));
    }

    /**
     * @brief Verifies unit test scenario: is feature allowed.
     */
    @Test
    void testIsFeatureAllowed() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        assertFalse(subscriptionService.isFeatureAllowed(user, "DEPLOY"));
        assertFalse(subscriptionService.isFeatureAllowed(user, "UNLIMITED_EVALS"));

        subscription.setTier(SubscriptionTier.EDUCATOR);
        assertTrue(subscriptionService.isFeatureAllowed(user, "DEPLOY"));
        assertTrue(subscriptionService.isFeatureAllowed(user, "OVERRIDE"));
        assertTrue(subscriptionService.isFeatureAllowed(user, "EXPORT"));
    }
}
