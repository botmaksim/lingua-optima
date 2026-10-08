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

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private UsageService usageService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("sub@lingua.com").build();
        subscription = Subscription.builder().id(UUID.randomUUID()).user(user).tier(SubscriptionTier.FREE).build();
    }

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

    @Test
    void testUpgradeToFreeDowngrades() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentResultResponse res = subscriptionService.upgrade(user, SubscriptionTier.FREE, null);
        assertTrue(res.isSuccess());
        assertEquals(SubscriptionTier.FREE, subscription.getTier());
    }

    @Test
    void testDowngrade() {
        subscription.setTier(SubscriptionTier.PREMIUM);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.downgrade(user);
        assertEquals(SubscriptionTier.FREE, subscription.getTier());
        assertNull(subscription.getExpiresAt());
    }

    @Test
    void testValidateCefrLevelAccess() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(subscription));

        // Free tier allows B1 and B2
        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.B1));
        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.B2));

        // Free tier blocks C1
        assertThrows(ForbiddenException.class, () -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.C1));

        // Premium allows C1
        subscription.setTier(SubscriptionTier.PREMIUM);
        assertDoesNotThrow(() -> subscriptionService.validateCefrLevelAccess(user, CefrLevel.C1));
    }

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
