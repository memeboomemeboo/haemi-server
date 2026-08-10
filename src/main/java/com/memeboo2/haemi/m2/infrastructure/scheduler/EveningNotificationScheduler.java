package com.memeboo2.haemi.m2.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.ElderRecipientResolver;
import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m2.domain.repository.MemoryPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 지연 로딩을 건드리지 않으므로 긴 읽기 트랜잭션을 열지 않는다.
 * 앨범은 페이지 단위로만 읽고, 각 조회가 자기 트랜잭션에서 끝난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EveningNotificationScheduler {

    private final NotificationPort notificationPort;
    private final AlbumBatchScanner albumScanner;
    private final MemoryPostRepository posts;
    private final ElderRecipientResolver elderRecipients;

    // F2-02: 매일 19:00 저녁 요약 알림
    @Scheduled(cron = "0 0 19 * * *")
    public void sendEveningSummary() {
        log.info("=== 저녁 요약 알림 발송 시작 ===");
        LocalDate today = LocalDate.now();
        // 한도 초과분과 미열람 글이 있는 앨범만, 연결된 어르신 기기로 한 번에 요약 발송한다.
        // 수신자 식별자 없이 가짜 기기로 발송하지 않는다.
        Set<String> recipients = new LinkedHashSet<>();
        albumScanner.forEachAlbum(album -> {
            if (posts.existsUnreadPublishedByAlbumIdSince(album.getId(), today.atStartOfDay())) {
                elderRecipients.resolveByGroupId(album.getGroupId()).ifPresent(recipients::add);
            }
        });

        recipients.forEach(elderId -> notificationPort.sendToElder(
                elderId,
                "오늘의 추억글 요약",
                "오늘 가족이 보낸 추억글을 확인해보세요 💌"));
        log.info("=== 저녁 요약 알림 발송 완료: 대상={}명 ===", recipients.size());
    }
}
