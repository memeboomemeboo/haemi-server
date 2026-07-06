package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock CognitiveMetricRepository metricRepository;
    @Mock CognitiveReportRepository reportRepository;
    @Mock CognitiveChangeAlertRepository alertRepository;
    @Mock AlbumRepository albumRepository;
    @Mock PdfReportPort pdfReportPort;
    @Mock NotificationPort notificationPort;

    DashboardApplicationService service;

    void setUp() {
        service = new DashboardApplicationService(
                metricRepository, reportRepository, alertRepository,
                albumRepository,
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

    @Test
    @DisplayName("리포트는 추이·회상·최다 사진 유형·이전 기간 변화를 집계하고 가족에게 알린다")
    void generateReport_buildsCompleteReportAndNotifiesFamily() {
        setUp();
        UUID albumId = UUID.randomUUID();
        Album album = Album.create("elder-1", "group-1", "family-1");
        album.inviteMember("family-2");
        List<CognitiveDailyMetric> current = metrics(albumId, 0.8, 20.0, "FAMILY");
        List<CognitiveDailyMetric> previous = metrics(albumId, 0.6, 25.0, "PORTRAIT");
        when(metricRepository.findByElderIdAndDateBetween(anyString(), any(), any()))
                .thenReturn(current)
                .thenReturn(previous);
        when(pdfReportPort.generatePdf(any())).thenReturn("/tmp/report.pdf");
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(albumRepository.findById(AlbumId.of(albumId))).thenReturn(Optional.of(album));

        var result = service.generateReport(new GenerateCognitiveReportCommand(
                "elder-1", albumId.toString(), ReportPeriod.WEEKLY,
                ReportDeliveryMethod.IN_APP));

        assertThat(result.accuracyTrend()).hasSize(7);
        assertThat(result.reminiscenceParticipationCount()).isEqualTo(14);
        assertThat(result.mostReactedPhotoType()).isEqualTo("FAMILY");
        assertThat(result.accuracyChangeFromPrevious()).isCloseTo(0.2,
                org.assertj.core.data.Offset.offset(0.0001));
        assertThat(result.responseTimeChangeFromPrevious()).isEqualTo(-5.0);
        verify(notificationPort).sendToGroup(
                album.getMemberIds(), "인지 리포트가 생성되었습니다", result.changeSummary());
    }

    @Test
    @DisplayName("리포트 최초 열람 시각을 저장한다")
    void markReportViewed_recordsViewedAt() {
        setUp();
        CognitiveReport report = CognitiveReport.create(
                "elder-1", null, ReportPeriod.WEEKLY,
                LocalDate.now().minusDays(7), LocalDate.now().minusDays(1),
                7, 0.8, 20.0, 3, 4, "FAMILY",
                0.1, -2.0, List.of(), "요약", ReportDeliveryMethod.IN_APP);
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(report)).thenReturn(report);

        var result = service.markReportViewed(report.getId().toString());

        assertThat(result.viewedAt()).isNotNull();
        verify(reportRepository).save(report);
    }

    private List<CognitiveDailyMetric> metrics(UUID albumId, double accuracy,
                                               double responseSeconds, String photoType) {
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> CognitiveDailyMetric.create(
                        "elder-1", albumId, "institution-1",
                        LocalDate.now().minusDays(index + 1),
                        1, accuracy, responseSeconds, 2, 1, photoType))
                .toList();
    }
}
