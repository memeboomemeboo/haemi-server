package com.memeboo2.haemi.m5.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CareReminderScheduler {

    // F5-01/F5-02: 실제 배포에서는 활성 알람과 산책 루틴을 분 단위로 조회해 FCM으로 발송한다.
    @Scheduled(cron = "0 * * * * *")
    public void scanDueReminders() {
        log.debug("공통 알림 스캔");
    }
}
