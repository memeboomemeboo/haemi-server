package com.memeboo2.haemi.m4.infrastructure.scheduler;

import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.service.DashboardApplicationService;
import com.memeboo2.haemi.m4.domain.model.dashboard.DataInsufficientException;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitiveReportSchedulerTest {

    @Mock
    DashboardApplicationService dashboardService;

    @Mock
    CognitiveMetricRepository metricRepository;

    CognitiveReportScheduler scheduler;

    void setUp() {
        scheduler = new CognitiveReportScheduler(dashboardService, metricRepository);
    }

    @Test
    @DisplayName("주간 리포트 스케줄러는 모든 어르신에게 주간 리포트를 생성한다")
    void generateWeeklyReports_iteratesAllElders() {
        setUp();
        when(metricRepository.findAllDistinctElderIds()).thenReturn(List.of("elder-1", "elder-2"));

        scheduler.generateWeeklyReports();

        verify(dashboardService, times(2)).generateReport(any(GenerateCognitiveReportCommand.class));
        verify(dashboardService).generateReport(argThat(cmd ->
                cmd.period() == ReportPeriod.WEEKLY && cmd.elderId().equals("elder-1")));
        verify(dashboardService).generateReport(argThat(cmd ->
                cmd.period() == ReportPeriod.WEEKLY && cmd.elderId().equals("elder-2")));
    }

    @Test
    @DisplayName("월간 리포트 스케줄러는 모든 어르신에게 월간 리포트를 생성한다")
    void generateMonthlyReports_iteratesAllElders() {
        setUp();
        when(metricRepository.findAllDistinctElderIds()).thenReturn(List.of("elder-1"));

        scheduler.generateMonthlyReports();

        verify(dashboardService).generateReport(argThat(cmd ->
                cmd.period() == ReportPeriod.MONTHLY && cmd.deliveryMethod() == ReportDeliveryMethod.IN_APP));
    }

    @Test
    @DisplayName("데이터 부족 시 예외를 잡아 다음 어르신으로 진행한다")
    void generateWeeklyReports_skipsDataInsufficientElders() {
        setUp();
        when(metricRepository.findAllDistinctElderIds()).thenReturn(List.of("elder-1", "elder-2"));
        when(dashboardService.generateReport(any()))
                .thenThrow(new DataInsufficientException())
                .thenReturn(null);

        scheduler.generateWeeklyReports();

        verify(dashboardService, times(2)).generateReport(any());
    }

    @Test
    @DisplayName("리포트 생성 중 런타임 예외가 발생해도 다음 어르신으로 진행한다")
    void generateWeeklyReports_skipsRuntimeErrorElders() {
        setUp();
        when(metricRepository.findAllDistinctElderIds()).thenReturn(List.of("elder-1", "elder-2"));
        when(dashboardService.generateReport(any()))
                .thenThrow(new RuntimeException("PDF generation failed"))
                .thenReturn(null);

        scheduler.generateWeeklyReports();

        verify(dashboardService, times(2)).generateReport(any());
    }

    @Test
    @DisplayName("어르신 목록이 비어 있으면 리포트를 생성하지 않는다")
    void generateWeeklyReports_emptyElderList() {
        setUp();
        when(metricRepository.findAllDistinctElderIds()).thenReturn(List.of());

        scheduler.generateWeeklyReports();

        verifyNoInteractions(dashboardService);
    }
}
