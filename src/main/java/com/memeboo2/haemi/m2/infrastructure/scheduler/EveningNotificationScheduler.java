package com.memeboo2.haemi.m2.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EveningNotificationScheduler {

    private final NotificationPort notificationPort;
    private final AlbumRepository albums;
    private final MemoryPostRepository posts;
    private final ElderRecipientResolver elderRecipients;

    // F2-02: 매일 19:00 저녁 요약 알림
    @Scheduled(cron = "0 0 19 * * *")
    public void sendEveningSummary() {
        log.info("=== 저녁 요약 알림 발송 시작 ===");
        // 한도 초과분과 미열람 글이 있는 앨범만, 연결된 어르신 기기로 한 번에 요약 발송한다.
        // 수신자 식별자 없이 가짜 기기로 발송하지 않는다.
        albums.findAll().stream()
                .filter(album -> posts.existsUnreadPublishedByAlbumIdSince(
                        album.getId(), LocalDate.now().atStartOfDay()))
                .map(album -> elderRecipients.resolveByGroupId(album.getGroupId()))
                .flatMap(Optional::stream)
                .distinct()
                .forEach(elderId -> notificationPort.sendToElder(
                        elderId,
                        "오늘의 추억글 요약",
                        "오늘 가족이 보낸 추억글을 확인해보세요 💌"));
    }
}
