/**
 * @file UsageServiceTest.java
 * @brief Unit and slice test suite for UsageService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.UsageCounter;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.response.UsageResponse;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UsageCounterRepository;
import com.linguaoptima.api.config.PricingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for UsageService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsageServiceTest {


    /** @brief Test fixture or mock dependency for usage counter repository. */
    @Mock
    private UsageCounterRepository usageCounterRepository;
    /** @brief Test fixture or mock dependency for subscription repository. */
    @Mock
    private SubscriptionRepository subscriptionRepository;
    /** @brief Test fixture or mock dependency for pricing configuration properties. */
    @Spy
    private PricingProperties pricingProperties = new PricingProperties();

    /** @brief Test fixture or mock dependency for usage service. */
    @InjectMocks
    private UsageService usageService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;
    /** @brief Test fixture or mock dependency for counter. */
    private UsageCounter counter;

    /**
     * @brief Initializes test fixtures and mock state before each test in UsageServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        counter = UsageCounter.builder().id(UUID.randomUUID()).user(user).weekEvaluations(0).weekOcrUploads(0).weekTokensUsed(0L).build();
        org.springframework.test.util.ReflectionTestUtils.setField(usageService, "pricingProperties", pricingProperties);
    }

    /**
     * @brief Verifies unit test scenario: increment evaluation free within limit.
     */
    @Test
    void testIncrementEvaluationFreeWithinLimit() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        usageService.incrementEvaluation(user);
        assertEquals(1, counter.getWeekEvaluations());
        verify(usageCounterRepository).save(counter);
    }

    /**
     * @brief Verifies unit test scenario: increment evaluation free limit reached throws.
     */
    @Test
    void testIncrementEvaluationFreeLimitReachedThrows() {
        counter.setWeekEvaluations(10);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        assertThrows(QuotaExceededException.class, () -> usageService.incrementEvaluation(user));
    }

    /**
     * @brief Verifies unit test scenario: increment evaluation premium unlimited.
     */
    @Test
    void testIncrementEvaluationPremiumWithLimits() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));
        assertDoesNotThrow(() -> usageService.incrementEvaluation(user));
        assertEquals(1, counter.getWeekEvaluations());
        verify(usageCounterRepository).save(counter);
    }

    /**
     * @brief Verifies unit test scenario: increment ocr free limit reached throws.
     */
    @Test
    void testIncrementOcrFreeLimitReachedThrows() {
        counter.setWeekOcrUploads(3);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        assertThrows(QuotaExceededException.class, () -> usageService.incrementOcr(user));
    }

    /**
     * @brief Verifies unit test scenario: increment ocr free within limit.
     */
    @Test
    void testIncrementOcrFreeWithinLimit() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        usageService.incrementOcr(user);
        assertEquals(1, counter.getWeekOcrUploads());
    }

    /**
     * @brief Verifies unit test scenario: get usage free and premium.
     */
    @Test
    void testGetUsageFreeAndPremium() {
        counter.setWeekEvaluations(4);
        counter.setWeekOcrUploads(1);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        UsageResponse freeUsage = usageService.getUsage(user);
        assertEquals(4, freeUsage.getWeekEvaluations());
        assertEquals(6, freeUsage.getEvaluationsRemaining());
        assertEquals(2, freeUsage.getOcrRemaining());

        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        UsageResponse premUsage = usageService.getUsage(user);
        assertEquals(500, premUsage.getEvaluationLimit());
        assertEquals(496, premUsage.getEvaluationsRemaining());
    }

    /**
     * @brief Verifies unit test scenario: reset weekly counters.
     */
    @Test
    void testResetWeeklyCounters() {
        when(usageCounterRepository.findAll()).thenReturn(List.of(counter));
        usageService.resetWeeklyCounters();
        assertEquals(0, counter.getWeekEvaluations());
        assertEquals(0, counter.getWeekOcrUploads());
        verify(usageCounterRepository).save(counter);

        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));
        assertDoesNotThrow(() -> usageService.incrementOcr(user));

        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(usageCounterRepository.save(any(UsageCounter.class))).thenReturn(counter);
        assertNotNull(usageService.getUsage(user));
        assertNotNull(usageService.getOrCreateCounter(user));
    }

    @Test
    void testConsumeTokens_SuccessAndQuotaExceeded() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        // Negative or zero tokens do nothing
        assertDoesNotThrow(() -> usageService.consumeTokens(user, 0L));
        assertDoesNotThrow(() -> usageService.consumeTokens(user, -5L));

        // Normal consumption
        usageService.consumeTokens(user, 500L);
        assertEquals(500L, counter.getWeekTokensUsed());
        verify(usageCounterRepository).save(counter);

        // Exceeding Free token limit (50,000)
        assertThrows(QuotaExceededException.class, () -> usageService.consumeTokens(user, 60_000L));
    }

    @Test
    void testEstimateTokens() {
        assertEquals(0L, UsageService.estimateTokens(null));
        assertEquals(0L, UsageService.estimateTokens(""));
        assertEquals(0L, UsageService.estimateTokens("   "));
        assertEquals(1L, UsageService.estimateTokens("hi"));
        assertEquals(5L, UsageService.estimateTokens("12345678901234567890"));
    }

    @Test
    void testIncrementOcr_WithCount() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        // Zero or negative count does nothing
        assertDoesNotThrow(() -> usageService.incrementOcr(user, 0));
        assertDoesNotThrow(() -> usageService.incrementOcr(user, -1));

        // Increment 2 pages
        usageService.incrementOcr(user, 2);
        assertEquals(2, counter.getWeekOcrUploads());

        // Increment beyond Free weekly OCR limit (3)
        assertThrows(QuotaExceededException.class, () -> usageService.incrementOcr(user, 2));
    }
}

