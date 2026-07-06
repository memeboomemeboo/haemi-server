package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.memeboo2.haemi.m4.domain.repository.CognitiveChangeAlertRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock CognitiveMetricRepository metricRepository;
    @Mock CognitiveReportRepository reportRepository;
    @Mock CognitiveChangeAlertRepository alertRepository;
    @Mock PdfReportPort pdfReportPort;
    @Mock NotificationPort notificationPort;

    DashboardApplicationService service;

    void setUp() {
        service = new DashboardApplicationService(
                metricRepository, reportRepository, alertRepository,
                pdfReportPort, notificationPort);
    }

    @Test
    @DisplayName("resolvePeriodEnd는 월간 리포트 시 전월 말일을 반환한다")
    void resolvePeriodEnd_monthlyReturnsLastDayOfPreviousMonth() throws Exception {
        setUp();
        Method method = DashboardApplicationService.class
                .getDeclaredMethod("resolvePeriodEnd", ReportPeriod.class);
        method.setAccessible(true);

        // When called on July 6, MONTHLY should return June 30
        var result = method.invoke(service, ReportPeriod.MONTHLY);
        // The result depends on the current date, so we verify the logic structurally
        assertThat((java.time.LocalDate) result).isEqualTo(
                java.time.LocalDate.now().minusMonths(1)
                        .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()));
    }

    @Test
    @DisplayName("resolvePeriodEnd는 주간 리포트 시 하루 전을 반환한다")
    void resolvePeriodEnd_weeklyReturnsYesterday() throws Exception {
        setUp();
        Method method = DashboardApplicationService.class
                .getDeclaredMethod("resolvePeriodEnd", ReportPeriod.class);
        method.setAccessible(true);

        var result = method.invoke(service, ReportPeriod.WEEKLY);
        assertThat((java.time.LocalDate) result)
                .isEqualTo(java.time.LocalDate.now().minusDays(1));
    }
}
