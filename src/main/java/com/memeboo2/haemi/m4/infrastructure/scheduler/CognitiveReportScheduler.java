package com.memeboo2.haemi.m4.infrastructure.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CognitiveReportScheduler {

    // F4-01: 매주 월요일 09:00 주간 리포트 생성 배치 진입점
    @Scheduled(cron = "0 0 9 * * MON")
    public void generateWeeklyReports() {
        log.info("=== 주간 인지 리포트 생성 시작 ===");
    }

    // F4-01: 매월 1일 09:00 월간 리포트 생성 배치 진입점
    @Scheduled(cron = "0 0 9 1 * *")
    public void generateMonthlyReports() {
        log.info("=== 월간 인지 리포트 생성 시작 ===");
    }
}
