/**
 * @file SchedulersTest.java
 * @brief Unit and slice test suite for Schedulers.
 */
package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.UserRepository;
import com.linguaoptima.api.service.GamificationService;
import com.linguaoptima.api.service.NotificationService;
import com.linguaoptima.api.service.UsageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for Schedulers.
 */
@ExtendWith(MockitoExtension.class)
class SchedulersTest {

    /** @brief Test fixture or mock dependency for gamification service. */
    @Mock
    private GamificationService gamificationService;
    /** @brief Test fixture or mock dependency for usage service. */
    @Mock
    private UsageService usageService;
    /** @brief Test fixture or mock dependency for user repository. */
    @Mock
    private UserRepository userRepository;
    /** @brief Test fixture or mock dependency for progress record repository. */
    @Mock
    private ProgressRecordRepository progressRecordRepository;
    /** @brief Test fixture or mock dependency for notification service. */
    @Mock
    private NotificationService notificationService;

    /**
     * @brief Verifies unit test scenario: streak scheduler.
     */
    @Test
    void testStreakScheduler() {
        StreakScheduler scheduler = new StreakScheduler(gamificationService);
        scheduler.runDailyStreakCheck();
        verify(gamificationService).applyDailyStreakCheck();
    }

    /**
     * @brief Verifies unit test scenario: usage reset scheduler.
     */
    @Test
    void testUsageResetScheduler() {
        UsageResetScheduler scheduler = new UsageResetScheduler(usageService);
        scheduler.resetWeeklyCounters();
        verify(usageService).resetWeeklyCounters();
    }

    /**
     * @brief Verifies unit test scenario: notification scheduler.
     */
    @Test
    void testNotificationScheduler() {
        User student = User.builder().id(UUID.randomUUID()).role(Role.STUDENT).build();
        ProgressRecord weak = ProgressRecord.builder().grammarTopic("Passive Voice").masteryScore(0.4).build();

        when(userRepository.findAll()).thenReturn(List.of(student));
        when(progressRecordRepository.findByStudentIdAndMasteryScoreLessThan(student.getId(), 0.6))
            .thenReturn(List.of(weak));

        NotificationScheduler scheduler = new NotificationScheduler(userRepository, progressRecordRepository, notificationService);
        scheduler.sendContextualReminders();

        verify(notificationService).send(eq(student), contains("Passive Voice"), eq(NotificationType.CONTEXTUAL));
    }
}
