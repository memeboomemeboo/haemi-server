package com.memeboo2.haemi.m2.infrastructure.scheduler;

import com.memeboo2.haemi.m2.application.command.ComputeRankingCommand;
import com.memeboo2.haemi.m2.application.service.RankingApplicationService;
import com.memeboo2.haemi.m2.domain.model.ranking.FamilyRanking;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;
import com.memeboo2.haemi.m2.domain.repository.FamilyRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingApplicationService rankingService;
    private final FamilyRankingRepository rankingRepository;

    // F2-04: 매주 월요일 00:00 주간 랭킹 집계
    @Scheduled(cron = "0 0 0 * * MON")
    public void computeWeeklyRanking() {
        log.info("=== 주간 랭킹 집계 시작 ===");
        LocalDate today    = LocalDate.now();
        LocalDate weekEnd  = today.minusDays(1);           // 지난 일요일
        LocalDate weekStart = weekEnd.minusDays(6);        // 7일 전 월요일

        computeForAllAlbums(RankingPeriod.WEEKLY, weekStart, weekEnd);
    }

    // F2-04: 매월 1일 00:00 월간 랭킹 집계
    @Scheduled(cron = "0 0 0 1 * *")
    public void computeMonthlyRanking() {
        log.info("=== 월간 랭킹 집계 시작 ===");
        LocalDate lastMonthEnd   = LocalDate.now().minusDays(1);
        LocalDate lastMonthStart = lastMonthEnd.with(TemporalAdjusters.firstDayOfMonth());

        computeForAllAlbums(RankingPeriod.MONTHLY, lastMonthStart, lastMonthEnd);
    }

    private void computeForAllAlbums(RankingPeriod period, LocalDate start, LocalDate end) {
        // 실제로는 모든 활성 앨범 ID 조회 후 배치 처리
        // 현재는 예시로 단일 처리만 구현
        log.info("기간: {} ~ {}, period={}", start, end, period);
    }
}
