package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.port.ElderAccessPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.domain.model.dashboard.*;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveChangeAlertRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardApplicationServiceTest {

    @Mock CognitiveMetricRepository metricRepository;
    @Mock CognitiveReportRepository reportRepository;
    @Mock CognitiveChangeAlertRepository alertRepository;
    @Mock AlertRecipientSettingRepository alertRecipientRepository;
    @Mock AlbumRepository albumRepository;
    @Mock PdfReportPort pdfReportPort;
    @Mock NotificationPort notificationPort;
    @Mock ElderAccessPort elderAccess;
    @Mock MemberRepository memberRepository;

    private DashboardApplicationService service;
    private UUID elderId;

    private void setUp() {
        elderId = UUID.randomUUID();
        service = new DashboardApplicationService(metricRepository, reportRepository, alertRepository,
                alertRecipientRepository, albumRepository, pdfReportPort,
                notificationPort, elderAccess, new ActivityChangeLanguagePolicy(), memberRepository);
    }

    @Test
    void resolvePeriodEndUsesYesterdayForWeeklyAndPreviousMonthEndForMonthly() throws Exception {
        setUp();
        Method method = DashboardApplicationService.class.getDeclaredMethod("resolvePeriodEnd", ReportPeriod.class);
        method.setAccessible(true);

        assertThat((LocalDate) method.invoke(service, ReportPeriod.WEEKLY)).isEqualTo(LocalDate.now().minusDays(1));
        assertThat((LocalDate) method.invoke(service, ReportPeriod.MONTHLY)).isEqualTo(
                LocalDate.now().minusMonths(1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth()));
    }

    @Test
    void standardReportContainsOnlyReminiscenceRecordsAndSafeActivityNarrative() {
        setUp();
        UUID albumId = UUID.randomUUID();
        Album album = Album.create(elderId.toString(), "group-1", "family-1");
        album.inviteMember("family-2");
        album.acceptInvite("family-2");
        when(elderAccess.getRequired(elderId)).thenReturn(snapshot(ElderStatus.ACTIVE));
        when(metricRepository.findByElderIdAndDateBetween(eq(elderId.toString()), any(), any()))
                .thenReturn(metrics(albumId, 1, "가족 여행", "photo-1"))
                .thenReturn(metrics(albumId, 1, "고향", "photo-2"));
        when(pdfReportPort.generatePdf(any())).thenReturn("/tmp/report.pdf");
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(albumRepository.findById(AlbumId.of(albumId))).thenReturn(Optional.of(album));

        var result = service.generateReport(new GenerateCognitiveReportCommand(
                elderId.toString(), albumId.toString(), ReportPeriod.WEEKLY, null, null, ReportDeliveryMethod.IN_APP));

        assertThat(result.mode()).isEqualTo(ReportMode.STANDARD);
        assertThat(result.daysTogether()).isEqualTo(7);
        assertThat(result.rememberedTopics()).containsExactly("가족 여행");
        assertThat(result.topDwelledPhotos()).containsExactly("photo-1");
        assertThat(result.activityMessage()).contains("함께한 날");
        assertThat(result.summary()).doesNotContain("정답", "점수", "인지", "악화", "개선");
        verify(notificationPort).sendToGroup(album.getMemberIds(), "회상 기록이 준비되었어요",
                result.summary() + " " + result.medicalDisclaimer());
    }

    @Test
    void decliningElderAlwaysGetsMemoryFocusedReportWithoutComparison() {
        setUp();
        UUID albumId = UUID.randomUUID();
        when(elderAccess.getRequired(elderId)).thenReturn(snapshot(ElderStatus.DECLINING));
        when(metricRepository.findByElderIdAndDateBetween(eq(elderId.toString()), any(), any()))
                .thenReturn(metrics(albumId, 1, "바닷가", "photo-1"));
        when(pdfReportPort.generatePdf(any())).thenReturn("/tmp/report.pdf");
        when(reportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(albumRepository.findById(AlbumId.of(albumId))).thenReturn(Optional.of(
                Album.create(elderId.toString(), "group-1", "family-1")));

        var result = service.generateReport(new GenerateCognitiveReportCommand(
                elderId.toString(), albumId.toString(), ReportPeriod.WEEKLY, null, null, ReportDeliveryMethod.IN_APP));

        assertThat(result.mode()).isEqualTo(ReportMode.MEMORY_FOCUSED);
        assertThat(result.activityMessage()).isNull();
        assertThat(result.summary()).doesNotContain("지난 기간", "비교", "정답", "점수");
        verify(metricRepository, times(1)).findByElderIdAndDateBetween(eq(elderId.toString()), any(), any());
    }

    @Test
    void deceasedElderBlocksReportBeforePdfOrNotification() {
        setUp();
        when(elderAccess.getRequired(elderId)).thenReturn(snapshot(ElderStatus.DECEASED));

        assertThatThrownBy(() -> service.generateReport(new GenerateCognitiveReportCommand(
                elderId.toString(), null, ReportPeriod.WEEKLY, null, null, ReportDeliveryMethod.IN_APP)))
                .isInstanceOf(ReportDeliveryBlockedException.class);

        verifyNoInteractions(metricRepository, pdfReportPort, reportRepository, notificationPort);
    }

    @Test
    void activityLanguageGateRejectsDiagnosticVocabulary() {
        assertThatThrownBy(() -> new ActivityChangeLanguagePolicy()
                .requireSafe("인지 기능이 악화되었습니다"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void activityAlertUsesVoiceChangeAndOnlySendsToInstitutionBeforeStageTwo() {
        setUp();
        AlertRecipientSetting setting = AlertRecipientSetting.createOrUpdate(
                null, elderId.toString(), "family-owner", Set.of("institution-manager"));
        when(elderAccess.getRequired(elderId)).thenReturn(snapshot(ElderStatus.ACTIVE));
        when(alertRecipientRepository.findByElderId(elderId.toString())).thenReturn(Optional.of(setting));
        when(alertRepository.findLatestByElderIdSince(eq(elderId.toString()), any())).thenReturn(Optional.empty());
        when(metricRepository.findByElderIdAndDateBetween(eq(elderId.toString()), any(), any()))
                .thenReturn(voiceDropHistory());
        when(alertRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var alerts = service.detectEarlyAlerts(elderId.toString());

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.alertType()).isEqualTo(AlertType.VOICE_ACTIVITY_DROP);
            assertThat(alert.message()).doesNotContain("정답", "점수", "인지", "악화", "진단");
        });
        verify(notificationPort).sendToGroup(Set.of("institution-manager"), "어르신 소식이에요",
                alerts.getFirst().message());
    }

    @Test
    @DisplayName("EX-F402-07: 사별 상태에서는 활동 변화 안내를 생성하거나 발송하지 않는다")
    void activityAlertIsNotSentAfterDeath() {
        setUp();
        when(elderAccess.getRequired(elderId)).thenReturn(snapshot(ElderStatus.DECEASED));

        assertThat(service.detectEarlyAlerts(elderId.toString())).isEmpty();
        verifyNoInteractions(alertRecipientRepository, metricRepository, notificationPort);
    }

    @Test
    void falsePositiveFeedbackIsPersistedForConservativeFutureDetection() {
        setUp();
        CognitiveChangeAlert alert = CognitiveChangeAlert.create(elderId.toString(), UUID.randomUUID(),
                AlertType.VOICE_ACTIVITY_DROP, "활동 변화 안내", "https://haemi.kr/guide");
        when(alertRepository.findById(alert.getId())).thenReturn(Optional.of(alert));
        when(alertRepository.save(alert)).thenReturn(alert);

        var result = service.markAlertFalsePositive(alert.getId().toString());

        assertThat(result.alertId()).isEqualTo(alert.getId().toString());
        assertThat(alert.getFalsePositiveAt()).isNotNull();
        verify(alertRepository).save(alert);
    }

    private ElderAccessPort.ElderAccessSnapshot snapshot(ElderStatus status) {
        return new ElderAccessPort.ElderAccessSnapshot(elderId, UUID.randomUUID(), status, ElderAccessMode.A, 2);
    }

    private List<CognitiveDailyMetric> metrics(UUID albumId, int voiceCount, String topic, String photo) {
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(index -> metric(albumId, LocalDate.now().minusDays(index + 1), 1, voiceCount, 5_000,
                        0, 0, 2, topic, photo))
                .toList();
    }

    private List<CognitiveDailyMetric> voiceDropHistory() {
        UUID albumId = UUID.randomUUID();
        List<CognitiveDailyMetric> metrics = new java.util.ArrayList<>();
        for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
            int voice = daysAgo <= 2 ? 0 : 1;
            metrics.add(metric(albumId, LocalDate.now().minusDays(daysAgo), 1, voice, 5_000,
                    0, 0, 0, null, null));
        }
        return metrics;
    }

    private CognitiveDailyMetric metric(UUID albumId, LocalDate date, int sessions, int voiceCount, double dwellMs,
                                        int hintPlaybacks, int hintNoResponses, int familyContributions,
                                        String topic, String photo) {
        CognitiveDailyMetric metric = CognitiveDailyMetric.create(elderId.toString(), albumId, "institution-1",
                date, 0, 0.0, 0.0, 0, 0, null);
        metric.updateReminiscenceSnapshot("institution-1", sessions, voiceCount, dwellMs,
                hintPlaybacks, hintNoResponses, familyContributions, topic, photo);
        return metric;
    }
}
