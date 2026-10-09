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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @file UsageServiceTest.java
 * @brief Unit and slice test suite for UsageService.
 */
@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageCounterRepository usageCounterRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private UsageService usageService;

    private User user;
    private UsageCounter counter;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).build();
        counter = UsageCounter.builder().id(UUID.randomUUID()).user(user).weekEvaluations(0).weekOcrUploads(0).build();
    }

    @Test
    void testIncrementEvaluationFreeWithinLimit() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        usageService.incrementEvaluation(user);
        assertEquals(1, counter.getWeekEvaluations());
        verify(usageCounterRepository).save(counter);
    }

    @Test
    void testIncrementEvaluationFreeLimitReachedThrows() {
        counter.setWeekEvaluations(10);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        assertThrows(QuotaExceededException.class, () -> usageService.incrementEvaluation(user));
    }

    @Test
    void testIncrementEvaluationPremiumUnlimited() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.PREMIUM).build()));
        assertDoesNotThrow(() -> usageService.incrementEvaluation(user));
        verify(usageCounterRepository, never()).save(any());
    }

    @Test
    void testIncrementOcrFreeLimitReachedThrows() {
        counter.setWeekOcrUploads(3);
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        assertThrows(QuotaExceededException.class, () -> usageService.incrementOcr(user));
    }

    @Test
    void testIncrementOcrFreeWithinLimit() {
        when(subscriptionRepository.findByUserId(user.getId())).thenReturn(Optional.of(Subscription.builder().tier(SubscriptionTier.FREE).build()));
        when(usageCounterRepository.findByUserId(user.getId())).thenReturn(Optional.of(counter));

        usageService.incrementOcr(user);
        assertEquals(1, counter.getWeekOcrUploads());
    }

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

    @Test
    void testResetWeeklyCounters() {
        when(usageCounterRepository.findAll()).thenReturn(List.of(counter));
        usageService.resetWeeklyCounters();
        assertEquals(0, counter.getWeekEvaluations());
        assertEquals(0, counter.getWeekOcrUploads());
        verify(usageCounterRepository).save(counter);
    }
}
