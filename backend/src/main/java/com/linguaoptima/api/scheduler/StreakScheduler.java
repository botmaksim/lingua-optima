/**
 * @file StreakScheduler.java
 * @brief Nightly scheduler task maintaining student study streaks and applying freeze token protections.
 */
package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @brief Nightly scheduler task maintaining student study streaks and applying freeze token protections.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreakScheduler {

    /** @brief Field representing gamification service in StreakScheduler. */
    private final GamificationService gamificationService;

    /**
     * @brief Executes daily at 01:00 AM to validate user activity streaks.
     *
     * Inspects active students: if inactive yesterday, automatically applies an available
     * freeze token or resets the streak count to zero.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void runDailyStreakCheck() {
        log.info("Starting scheduled streak validation job at 01:00");
        gamificationService.applyDailyStreakCheck();
    }
}
