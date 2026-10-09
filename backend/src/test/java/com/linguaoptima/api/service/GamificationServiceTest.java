/**
 * @file GamificationServiceTest.java
 * @brief Unit and slice test suite for GamificationService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for GamificationService.
 */
@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    /** @brief Test fixture or mock dependency for user repository. */
    @Mock
    private UserRepository userRepository;

    /** @brief Test fixture or mock dependency for notification service. */
    @Mock
    private NotificationService notificationService;

    /** @brief Test fixture or mock dependency for gamification service. */
    @InjectMocks
    private GamificationService gamificationService;

    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in GamificationServiceTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(UUID.randomUUID())
            .email("game@lingua.com")
            .streakCount(0)
            .freezeTokens(0)
            .build();
    }

    /**
     * @brief Verifies unit test scenario: on submission completed first time.
     */
    @Test
    void testOnSubmissionCompletedFirstTime() {
        gamificationService.onSubmissionCompleted(user);
        assertEquals(1, user.getStreakCount());
        assertEquals(LocalDate.now(), user.getLastActiveDate());
        verify(userRepository).save(user);
    }

    /**
     * @brief Verifies unit test scenario: on submission completed next day increments streak.
     */
    @Test
    void testOnSubmissionCompletedNextDayIncrementsStreak() {
        user.setStreakCount(3);
        user.setLastActiveDate(LocalDate.now().minusDays(1));

        gamificationService.onSubmissionCompleted(user);
        assertEquals(4, user.getStreakCount());
        verify(userRepository).save(user);
    }

    /**
     * @brief Verifies unit test scenario: on submission completed7 day milestone grants freeze token.
     */
    @Test
    void testOnSubmissionCompleted7DayMilestoneGrantsFreezeToken() {
        user.setStreakCount(6);
        user.setLastActiveDate(LocalDate.now().minusDays(1));

        gamificationService.onSubmissionCompleted(user);
        assertEquals(7, user.getStreakCount());
        assertEquals(1, user.getFreezeTokens());
        verify(notificationService).send(eq(user), anyString(), eq(NotificationType.SYSTEM));
    }

    /**
     * @brief Verifies unit test scenario: on submission completed same day does not increment.
     */
    @Test
    void testOnSubmissionCompletedSameDayDoesNotIncrement() {
        user.setStreakCount(5);
        user.setLastActiveDate(LocalDate.now());

        gamificationService.onSubmissionCompleted(user);
        assertEquals(5, user.getStreakCount());

        user.setLastActiveDate(LocalDate.now().minusDays(3));
        gamificationService.onSubmissionCompleted(user);
        assertEquals(1, user.getStreakCount());
    }


    /**
     * @brief Verifies unit test scenario: apply daily streak check uses freeze token.
     */
    @Test
    void testApplyDailyStreakCheckUsesFreezeToken() {
        user.setStreakCount(10);
        user.setFreezeTokens(2);
        user.setLastActiveDate(LocalDate.now().minusDays(2));

        when(userRepository.findAll()).thenReturn(List.of(user));

        gamificationService.applyDailyStreakCheck();

        assertEquals(10, user.getStreakCount());
        assertEquals(1, user.getFreezeTokens());
        verify(notificationService).send(eq(user), anyString(), eq(NotificationType.SYSTEM));
        verify(userRepository).save(user);
    }

    /**
     * @brief Verifies unit test scenario: apply daily streak check resets streak when no tokens.
     */
    @Test
    void testApplyDailyStreakCheckResetsStreakWhenNoTokens() {
        user.setStreakCount(10);
        user.setFreezeTokens(0);
        user.setLastActiveDate(LocalDate.now().minusDays(2));

        when(userRepository.findAll()).thenReturn(List.of(user));

        gamificationService.applyDailyStreakCheck();

        assertEquals(0, user.getStreakCount());
        verify(notificationService).send(eq(user), anyString(), eq(NotificationType.SYSTEM));
        verify(userRepository).save(user);
    }
}
