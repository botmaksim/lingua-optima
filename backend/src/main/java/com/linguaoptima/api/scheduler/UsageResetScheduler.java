package com.linguaoptima.api.scheduler;

import com.linguaoptima.api.service.UsageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsageResetScheduler {

    private final UsageService usageService;

    /**
     * Executes every Monday at 00:00 AM:
     * Resets weekly evaluation and OCR counters for free users.
     */
    @Scheduled(cron = "0 0 0 * * MON")
    public void resetWeeklyCounters() {
        log.info("Starting scheduled weekly usage counter reset on Monday 00:00");
        usageService.resetWeeklyCounters();
    }
}
