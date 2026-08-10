package com.memeboo2.haemi.m3.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTrainingScheduler {

    private final NotificationPort notificationPort;
    private final AlbumRepository albumRepository;
    private final ElderRecipientResolver elderRecipients;

    // F3-01: 매일 오전 09:00 오늘의 훈련 알림
    @Scheduled(
            cron = "${haemi.training.daily-reminder-cron:0 0 9 * * *}",
            zone = "${haemi.training.time-zone:Asia/Seoul}"
    )
    @Transactional(readOnly = true)
    public void sendDailyTrainingReminder() {
        log.info("=== 일일 인지 훈련 알림 발송 시작 ===");
        List<String> eligibleElderIds = albumRepository.findAll().stream()
                .filter(album -> album.getPhotos().size() >= 5)
                .map(Album::getGroupId)
                .map(elderRecipients::resolveByGroupId)
                .flatMap(Optional::stream)
                .distinct()
                .toList();

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
