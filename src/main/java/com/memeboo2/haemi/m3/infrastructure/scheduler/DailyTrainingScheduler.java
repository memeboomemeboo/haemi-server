package com.memeboo2.haemi.m3.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 사진 수 조건을 쿼리로 내려 사진 컬렉션을 지연 로딩하지 않는다.
 * 그래서 순회 전체를 감싸는 읽기 트랜잭션이 필요 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTrainingScheduler {

    private static final int MIN_PHOTOS_FOR_TRAINING = 5;

    private final NotificationPort notificationPort;
    private final AlbumBatchScanner albumScanner;
    private final ElderRecipientResolver elderRecipients;

    // F3-01: 매일 오전 09:00 오늘의 훈련 알림
    @Scheduled(
            cron = "${haemi.training.daily-reminder-cron:0 0 9 * * *}",
            zone = "${haemi.training.daily-reminder-zone:Asia/Seoul}"
    )
    public void sendDailyTrainingReminder() {
        log.info("=== 일일 인지 훈련 알림 발송 시작 ===");
        Set<String> eligibleElderIds = new LinkedHashSet<>();
        albumScanner.forEachAlbumWithAtLeastPhotos(MIN_PHOTOS_FOR_TRAINING, album ->
                elderRecipients.resolveByGroupId(album.getGroupId()).ifPresent(eligibleElderIds::add));

        eligibleElderIds.forEach(elderId ->
                notificationPort.sendToElder(
                        elderId,
                        "오늘의 인지 훈련",
                        "오늘의 훈련을 시작해볼까요?"
                )
        );
        log.info("일일 인지 훈련 알림 발송 완료: 대상={}명", eligibleElderIds.size());
    }
}
