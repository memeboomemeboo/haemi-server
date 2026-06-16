package com.memeboo2.haemi.m3.infrastructure.scheduler;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTrainingScheduler {

    private final NotificationPort notificationPort;

    // F3-01: 매일 오전 09:00 오늘의 훈련 알림
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyTrainingReminder() {
        log.info("=== 일일 인지 훈련 알림 발송 시작 ===");
        notificationPort.sendToMember(
                "elder-device",
                "오늘의 인지 훈련",
                "오늘의 훈련을 시작해볼까요?"
        );
    }
}
