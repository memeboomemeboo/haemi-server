package com.memeboo2.haemi.eventlog.infrastructure;

import com.memeboo2.haemi.eventlog.application.EventLoggingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 06:00 일일 이벤트 집계 스케줄러 (F0-06). 전일 이벤트를 타입별로 집계한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventAggregationScheduler {

    private final EventLoggingService eventLoggingService;

    @Scheduled(
            cron = "${haemi.eventlog.aggregate-cron:0 0 6 * * *}",
            zone = "${haemi.eventlog.time-zone:Asia/Seoul}"
    )
    public void aggregateYesterday() {
        eventLoggingService.aggregateDaily(LocalDate.now().minusDays(1));
    }
}
