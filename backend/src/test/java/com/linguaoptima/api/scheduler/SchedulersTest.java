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
 * @file SchedulersTest.java
 * @brief Unit and slice test suite for Schedulers.
 */
@ExtendWith(MockitoExtension.class)
class SchedulersTest {

    @Mock
    private GamificationService gamificationService;
    @Mock
    private UsageService usageService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgressRecordRepository progressRecordRepository;
    @Mock
    private NotificationService notificationService;

    @Test
    void testStreakScheduler() {
        StreakScheduler scheduler = new StreakScheduler(gamificationService);
        scheduler.runDailyStreakCheck();
        verify(gamificationService).applyDailyStreakCheck();
    }

    @Test
    void testUsageResetScheduler() {
        UsageResetScheduler scheduler = new UsageResetScheduler(usageService);
        scheduler.resetWeeklyCounters();
        verify(usageService).resetWeeklyCounters();
    }

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
