package com.memeboo2.haemi.m1.infrastructure.scheduler;

import com.memeboo2.haemi.m1.application.service.AlbumBatchScanner;
import com.memeboo2.haemi.m1.application.service.ReminiscenceApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminiscenceScheduler {

    private final AlbumBatchScanner albumScanner;
    private final ReminiscenceApplicationService reminiscenceService;

    // F1-05: 매일 오전 08:00 자동 실행
    @Scheduled(cron = "${haemi.reminiscence.cron:0 0 8 * * *}",
            zone = "${haemi.reminiscence.time-zone:Asia/Seoul}")
    public void generateDailyReminiscence() {
        log.info("=== 오늘의 회상 일괄 생성 시작 ===");
        AtomicInteger success = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        albumScanner.forEachAlbum(album -> {
            try {
                if (reminiscenceService.generateTodayReminiscence(album.getId().toString()).isPresent()) {
                    success.incrementAndGet();
                } else {
                    skipped.incrementAndGet();
                }
            } catch (Exception e) {
                // 앨범 한 건의 실패가 나머지 앨범 생성을 멈추게 두지 않는다.
                failed.incrementAndGet();
                log.error("회상 생성 실패: albumId={}", album.getId(), e);
            }
        });

        log.info("=== 오늘의 회상 생성 완료: success={}, skipped={}, failed={} ===",
                success.get(), skipped.get(), failed.get());
    }
}
