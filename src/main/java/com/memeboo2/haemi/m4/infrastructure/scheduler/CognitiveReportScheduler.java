package com.memeboo2.haemi.m4.infrastructure.scheduler;

import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.service.DashboardApplicationService;
import com.memeboo2.haemi.m4.domain.model.dashboard.DataInsufficientException;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CognitiveReportScheduler {

    private final DashboardApplicationService dashboardService;
    private final CognitiveMetricRepository metricRepository;

    // F4-01: 매주 월요일 09:00 주간 리포트 생성 배치
    @Scheduled(cron = "0 0 9 * * MON")
    public void generateWeeklyReports() {
        log.info("=== 주간 인지 리포트 생성 시작 ===");
        List<String> elderIds = metricRepository.findAllDistinctElderIds();
        log.info("대상 어르신 수: {}", elderIds.size());
        for (String elderId : elderIds) {
            try {
                dashboardService.generateReport(new GenerateCognitiveReportCommand(
                        elderId, null, ReportPeriod.WEEKLY, null, null, ReportDeliveryMethod.IN_APP));
                log.info("주간 리포트 생성 완료: elderId={}", elderId);
            } catch (DataInsufficientException e) {
                log.info("데이터 부족으로 리포트 생략: elderId={}", elderId);
            } catch (RuntimeException e) {
                log.error("주간 리포트 생성 실패: elderId={}", elderId, e);
            }
        }
        log.info("=== 주간 인지 리포트 생성 종료 ===");
    }

    // F4-01: 매월 1일 09:00 월간 리포트 생성 배치
    @Scheduled(cron = "0 0 9 1 * *")
    public void generateMonthlyReports() {
        log.info("=== 월간 인지 리포트 생성 시작 ===");
        List<String> elderIds = metricRepository.findAllDistinctElderIds();
        log.info("대상 어르신 수: {}", elderIds.size());
        for (String elderId : elderIds) {
            try {
                dashboardService.generateReport(new GenerateCognitiveReportCommand(
                        elderId, null, ReportPeriod.MONTHLY, null, null, ReportDeliveryMethod.IN_APP));
                log.info("월간 리포트 생성 완료: elderId={}", elderId);
            } catch (DataInsufficientException e) {
                log.info("데이터 부족으로 리포트 생략: elderId={}", elderId);
            } catch (RuntimeException e) {
                log.error("월간 리포트 생성 실패: elderId={}", elderId, e);
            }
        }
        log.info("=== 월간 인지 리포트 생성 종료 ===");
    }
}
