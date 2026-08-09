package com.memeboo2.haemi.predownload.infrastructure;

import com.memeboo2.haemi.predownload.application.PredownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 선다운로드 스케줄러 (#48). 배치 순서: 08:00 콘텐츠 생성 → 08:45 선다운로드 → 09:00 알림.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredownloadScheduler {

    private final PredownloadService predownloadService;

    @Scheduled(
            cron = "${haemi.predownload.cron:0 45 8 * * *}",
            zone = "${haemi.predownload.time-zone:Asia/Seoul}"
    )
    public void runDailyPredownload() {
        log.info("=== 선다운로드 파이프라인 시작 ===");
        predownloadService.runDailyPredownload(LocalDate.now());
    }
}
