package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.UserRepository;
import com.linguaoptima.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @file NotificationScheduler.java
 * @brief Scheduled background component dispatching daily contextual learning notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final ProgressRecordRepository progressRecordRepository;
    private final NotificationService notificationService;

    /**
     * @brief Executes daily at 09:00 AM to analyze student ProgressRecords and send contextual recommendations for weak topics (mastery &lt; 0.6).
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendContextualReminders() {
        log.info("Starting scheduled contextual notification dispatch at 09:00");
        List<User> students = userRepository.findAll().stream()
            .filter(u -> u.getRole() == Role.STUDENT)
            .toList();

        for (User student : students) {
            List<ProgressRecord> weakRecords = progressRecordRepository.findByStudentIdAndMasteryScoreLessThan(student.getId(), 0.6);
            if (!weakRecords.isEmpty()) {
                ProgressRecord weakest = weakRecords.get(0);
                notificationService.send(student,
                    "🎯 Practice tip: You had difficulties with '" + weakest.getGrammarTopic() +
                    "'. Generate a targeted practice set today to improve!",
                    NotificationType.CONTEXTUAL);
            }
        }
    }
}
