package com.memeboo2.haemi.m2.infrastructure.scheduler;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class EveningNotificationScheduler {

    private final NotificationPort notificationPort;

    // F2-02: 매일 19:00 저녁 요약 알림
    @Scheduled(cron = "0 0 19 * * *")
    public void sendEveningSummary() {
        log.info("=== 저녁 요약 알림 발송 시작 ===");
        // 실제로는 앨범별로 당일 미수신 추억글 목록을 조회하여 요약 발송
        // 한도 초과분 + 미열람 추억글을 1건으로 묶어서 발송
        notificationPort.sendToGroup(
                Set.of("elder-device"),
                "오늘의 추억글 요약",
                "오늘 가족이 보낸 추억글을 확인해보세요 💌"
        );
    }
}
