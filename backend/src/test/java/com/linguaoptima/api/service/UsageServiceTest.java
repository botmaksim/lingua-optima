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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        counter = UsageCounter.builder().id(UUID.randomUUID()).user(user).weekEvaluations(0).weekOcrUploads(0).build();
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
    void testIncrementEvaluationPremiumUnlimited() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        assertDoesNotThrow(() -> usageService.incrementEvaluation(user));
        verify(usageCounterRepository, never()).save(any());
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
        assertNull(premUsage.getEvaluationLimit());
        assertEquals(Integer.MAX_VALUE, premUsage.getEvaluationsRemaining());
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
        assertDoesNotThrow(() -> usageService.incrementOcr(user));

        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(usageCounterRepository.save(any(UsageCounter.class))).thenReturn(counter);
        assertNotNull(usageService.getUsage(user));
        assertNotNull(usageService.getOrCreateCounter(user));
    }
}

