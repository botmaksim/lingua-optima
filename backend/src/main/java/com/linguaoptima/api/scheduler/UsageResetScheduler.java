/**
 * @file UsageResetScheduler.java
 * @brief Weekly scheduler task resetting free-tier usage quota counters.
 */
package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @brief Weekly scheduler task resetting free-tier usage quota counters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsageResetScheduler {

    /** @brief Field representing usage service in UsageResetScheduler. */
    private final UsageService usageService;

    /**
     * @brief Executes weekly on Monday at 00:00 AM UTC.
     *
     * Resets weekly evaluation counters and OCR upload limits for all registered users.
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void resetWeeklyCounters() {
        log.info("Starting scheduled weekly usage counter reset on Monday 00:00");
        usageService.resetWeeklyCounters();
    }
}
