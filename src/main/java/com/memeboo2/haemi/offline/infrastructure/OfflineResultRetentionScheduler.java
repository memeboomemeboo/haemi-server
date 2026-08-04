package com.memeboo2.haemi.offline.infrastructure;

import com.memeboo2.haemi.offline.application.OfflineResultIngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 오프라인 결과 영수증 7일 보관 정리 스케줄러 (#49).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineResultRetentionScheduler {

    private final OfflineResultIngestService ingestService;

    @Scheduled(
            cron = "${haemi.offline.cleanup-cron:0 0 4 * * *}",
            zone = "${haemi.offline.time-zone:Asia/Seoul}"
    )
    public void purgeExpiredReceipts() {
        ingestService.purgeExpired(LocalDateTime.now());
    }
}
