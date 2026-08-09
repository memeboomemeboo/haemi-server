package com.memeboo2.haemi.m0.infrastructure.scheduler;

import com.memeboo2.haemi.m0.application.service.AccessModeApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 접근 모드 14일 주기 재평가 스케줄러 (F0-03). 제안만 생성하며 모드를 임의 변경하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessModeReviewScheduler {

    private final AccessModeApplicationService accessModeService;

    @Scheduled(
            cron = "${haemi.access-mode.review-cron:0 30 6 * * *}",
            zone = "${haemi.access-mode.time-zone:Asia/Seoul}"
    )
    public void proposePeriodicReviews() {
        accessModeService.runPeriodicReview(LocalDateTime.now());
    }
}
