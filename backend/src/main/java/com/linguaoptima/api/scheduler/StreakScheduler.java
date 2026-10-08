package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StreakScheduler {

    private final GamificationService gamificationService;

    /**
     * Executes daily at 01:00 AM:
     * Checks users' last active date, uses freeze tokens or resets streaks.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void runDailyStreakCheck() {
        log.info("Starting scheduled streak validation job at 01:00");
        gamificationService.applyDailyStreakCheck();
    }
}
