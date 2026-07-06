package com.memeboo2.haemi.m5.infrastructure.scheduler;

import com.memeboo2.haemi.m5.application.service.CareApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CareReminderScheduler {

    private final CareApplicationService careApplicationService;

    @Scheduled(cron = "0 * * * * *")
    public void scanDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("공통 알림 스캔: now={}", now);
        careApplicationService.processDueReminders(now);
    }
}
