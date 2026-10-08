package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void onSubmissionCompleted(User user) {
        LocalDate today = LocalDate.now();
        LocalDate lastActive = user.getLastActiveDate();

        if (lastActive == null) {
            user.setStreakCount(1);
        } else if (lastActive.equals(today.minusDays(1))) {
            user.setStreakCount(user.getStreakCount() + 1);
            if (user.getStreakCount() % 7 == 0) {
                user.setFreezeTokens(user.getFreezeTokens() + 1);
                notificationService.send(user,
                    "🔥 7-Day streak milestone reached! You earned 1 Streak Freeze token.",
                    NotificationType.SYSTEM);
            }
        } else if (!lastActive.equals(today)) {
            user.setStreakCount(1);
        }

        user.setLastActiveDate(today);
        userRepository.save(user);
    }

    @Transactional
    public void applyDailyStreakCheck() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Running daily streak check for activity on or before {}", yesterday);

        userRepository.findAll().forEach(user -> {
            LocalDate lastActive = user.getLastActiveDate();
            if (user.getStreakCount() > 0 && (lastActive == null || lastActive.isBefore(yesterday))) {
                if (user.getFreezeTokens() > 0) {
                    user.setFreezeTokens(user.getFreezeTokens() - 1);
                    user.setLastActiveDate(yesterday); // Protected by freeze token
                    userRepository.save(user);
                    notificationService.send(user,
                        "❄️ A Streak Freeze token was used to protect your " + user.getStreakCount() + "-day streak!",
                        NotificationType.SYSTEM);
                    log.info("Streak freeze token used for user {}", user.getEmail());
                } else {
                    user.setStreakCount(0);
                    userRepository.save(user);
                    notificationService.send(user,
                        "💔 Your streak was reset. Complete a task today to start a new streak!",
                        NotificationType.SYSTEM);
                    log.info("Streak reset to 0 for user {}", user.getEmail());
                }
            }
        });
    }
}
