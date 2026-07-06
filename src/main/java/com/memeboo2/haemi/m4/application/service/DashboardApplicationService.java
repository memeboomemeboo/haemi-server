package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.command.RecordCognitiveMetricCommand;
import com.memeboo2.haemi.m4.application.command.UpdateAlertRecipientsCommand;
import com.memeboo2.haemi.m4.application.dto.*;
import com.memeboo2.haemi.m4.application.query.GetCognitiveMetricQuery;
import com.memeboo2.haemi.m4.application.query.GetInstitutionDashboardQuery;
import com.memeboo2.haemi.m4.domain.model.dashboard.*;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.memeboo2.haemi.m4.domain.port.InstitutionDashboardExportPort;
import com.memeboo2.haemi.m4.domain.repository.CognitiveChangeAlertRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private static final String GUIDE_LINK = "https://haemi.kr/guide/cognitive-change";

    private final CognitiveMetricRepository metricRepository;
    private final CognitiveReportRepository reportRepository;
    private final CognitiveChangeAlertRepository alertRepository;
    private final AlertRecipientSettingRepository alertRecipientRepository;
    private final AlbumRepository albumRepository;
    private final PdfReportPort pdfReportPort;
    private final InstitutionDashboardExportPort institutionDashboardExportPort;
    private final NotificationPort notificationPort;

    @Transactional
    public CognitiveMetricResult recordMetric(RecordCognitiveMetricCommand command) {
        LocalDate date = command.metricDate() != null ? command.metricDate() : LocalDate.now();
        CognitiveDailyMetric metric = metricRepository.findByElderIdAndMetricDate(command.elderId(), date)
                .orElseGet(() -> CognitiveDailyMetric.create(
                        command.elderId(),
                        command.albumId() != null ? UUID.fromString(command.albumId()) : null,
                        command.institutionId(),
                        date,
                        0,
                        0.0,
                        0.0,
                        0,
                        0,
                        null
                ));
        metric.updateSnapshot(command.institutionId(), command.trainingSessionCount(),
                command.trainingAccuracyRate(), command.averageResponseSeconds(),
                command.reminiscenceReactionCount(), command.memoryPostCount(),
                command.mostReactedPhotoType());
        return CognitiveMetricResult.from(metricRepository.save(metric));
    }

    @Transactional
    public void recordTrainingCompletion(String elderId, UUID albumId, LocalDate metricDate,
                                         double accuracyRate, double averageResponseSeconds) {
        CognitiveDailyMetric metric = metricRepository.findByElderIdAndMetricDate(elderId, metricDate)
                .orElseGet(() -> CognitiveDailyMetric.create(
                        elderId, albumId, null, metricDate, 0, 0.0,
                        0.0, 0, 0, null));
        metric.mergeTrainingResult(albumId, accuracyRate, averageResponseSeconds);
        metricRepository.save(metric);
    }

    @Transactional(readOnly = true)
    public List<CognitiveMetricResult> getMetrics(GetCognitiveMetricQuery query) {
        List<CognitiveDailyMetric> metrics = metricRepository.findByElderIdAndDateBetween(
                query.elderId(), query.from(), query.to());
        if (metrics.isEmpty()) {
            throw new CognitiveMetricNotFoundException(query.elderId());
        }
        return metrics.stream().map(CognitiveMetricResult::from).toList();
    }

    // F4-01: 주간·월간 리포트 생성
    @Transactional
    public CognitiveReportResult generateReport(GenerateCognitiveReportCommand command) {
        LocalDate end = resolvePeriodEnd(command.period());
        LocalDate start = command.period() == ReportPeriod.WEEKLY
                ? end.minusDays(6)
                : end.with(TemporalAdjusters.firstDayOfMonth());

        List<CognitiveDailyMetric> metrics = metricRepository.findByElderIdAndDateBetween(
                command.elderId(), start, end);
        if (metrics.size() < 7) {
            throw new DataInsufficientException();
        }

        int participationCount = metrics.stream().mapToInt(CognitiveDailyMetric::getTrainingSessionCount).sum();
        double avgAccuracy = metrics.stream().mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate).average().orElse(0.0);
        double avgResponse = metrics.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0);
        int memoryPosts = metrics.stream().mapToInt(CognitiveDailyMetric::getMemoryPostCount).sum();
        int reminiscenceParticipationCount = metrics.stream()
                .mapToInt(CognitiveDailyMetric::getReminiscenceReactionCount)
                .sum();
        String mostReactedPhotoType = metrics.stream()
                .filter(metric -> metric.getMostReactedPhotoType() != null)
                .filter(metric -> !metric.getMostReactedPhotoType().isBlank())
                .collect(Collectors.groupingBy(
                        CognitiveDailyMetric::getMostReactedPhotoType,
                        LinkedHashMap::new,
                        Collectors.summingInt(CognitiveDailyMetric::getReminiscenceReactionCount)
                ))
                .entrySet().stream()
                .max(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(null);

        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(periodDays - 1);
        List<CognitiveDailyMetric> previousMetrics = metricRepository.findByElderIdAndDateBetween(
                command.elderId(), previousStart, previousEnd);
        double previousAccuracy = previousMetrics.stream()
                .mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate)
                .average()
                .orElse(avgAccuracy);
        double previousResponse = previousMetrics.stream()
                .mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds)
                .average()
                .orElse(avgResponse);
        double accuracyChange = avgAccuracy - previousAccuracy;
        double responseChange = avgResponse - previousResponse;

        String summary = buildChangeSummary(
                start, end, avgAccuracy, avgResponse, accuracyChange, responseChange,
                previousMetrics.isEmpty());
        UUID albumId = resolveAlbumId(command, metrics);
        List<ReportTrendPoint> trend = metrics.stream()
                .sorted(Comparator.comparing(CognitiveDailyMetric::getMetricDate))
                .map(metric -> new ReportTrendPoint(
                        metric.getMetricDate(), metric.getTrainingAccuracyRate()))
                .toList();
        CognitiveReport report = CognitiveReport.create(
                command.elderId(),
                albumId,
                command.period(), start, end, participationCount, avgAccuracy,
                avgResponse, memoryPosts, reminiscenceParticipationCount,
                mostReactedPhotoType, accuracyChange, responseChange, trend,
                summary, command.deliveryMethod());
        String pdfKey = pdfReportPort.generatePdf(report);
        report.assignPdfKey(pdfKey);
        CognitiveReport saved = reportRepository.save(report);

        sendReportNotification(saved);
        return CognitiveReportResult.from(saved);
    }

    @Transactional
    public CognitiveReportResult markReportViewed(String reportId) {
        CognitiveReport report = loadReport(reportId);
        report.markViewed(LocalDateTime.now());
        return CognitiveReportResult.from(reportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public CognitiveReport getReport(String reportId) {
        return loadReport(reportId);
    }

    // F4-02: 조기 알림 트리거 검사
    @Transactional
    public AlertRecipientSettingResult updateAlertRecipients(UpdateAlertRecipientsCommand command) {
        AlertRecipientSetting existing = alertRecipientRepository.findByElderId(command.elderId())
                .orElse(null);
        AlertRecipientSetting setting = AlertRecipientSetting.createOrUpdate(
                existing,
                command.elderId(),
                command.primaryCaregiverMemberId(),
                command.institutionManagerMemberIds()
        );
        return AlertRecipientSettingResult.from(alertRecipientRepository.save(setting));
    }

    @Transactional(readOnly = true)
    public AlertRecipientSettingResult getAlertRecipients(String elderId) {
        return AlertRecipientSettingResult.from(loadAlertRecipients(elderId));
    }

    @Transactional
    public List<CognitiveAlertResult> detectEarlyAlerts(String elderId) {
        AlertRecipientSetting recipients = loadAlertRecipients(elderId);
        LocalDate today = LocalDate.now();
        List<CognitiveDailyMetric> recent = metricRepository.findByElderIdAndDateBetween(
                elderId, today.minusDays(6), today);
        if (recent.size() < 7) {
            throw new DataInsufficientException();
        }
        if (alertRepository.findLatestByElderIdSince(elderId, LocalDateTime.now().minusDays(7)).isPresent()) {
            return List.of();
        }

        List<CognitiveChangeAlert> alerts = new ArrayList<>();
        if (recent.stream().noneMatch(CognitiveDailyMetric::participated)) {
            alerts.add(createAlert(recent.get(0), AlertType.NO_PARTICIPATION_7_DAYS,
                    "최근 7일 동안 세션 참여가 없습니다. 의료 진단이 아니며, 생활 패턴 변화를 함께 확인해주세요.",
                    recipients));
            return alerts.stream().map(CognitiveAlertResult::from).toList();
        }

        List<CognitiveDailyMetric> previous = metricRepository.findByElderIdAndDateBetween(
                elderId, today.minusDays(13), today.minusDays(7));
        if (previous.size() >= 7) {
            double previousAccuracy = previous.stream().mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate).average().orElse(0.0);
            double previousResponse = previous.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0);
            List<CognitiveDailyMetric> lastThreeDays = recent.stream()
                    .filter(metric -> !metric.getMetricDate().isBefore(today.minusDays(2)))
                    .sorted(Comparator.comparing(CognitiveDailyMetric::getMetricDate))
                    .toList();

            if (lastThreeDays.size() == 3
                    && previousAccuracy > 0
                    && lastThreeDays.stream().allMatch(
                    metric -> metric.participated()
                            && previousAccuracy - metric.getTrainingAccuracyRate() >= 0.20)) {
                double recentAccuracy = lastThreeDays.stream()
                        .mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate)
                        .average()
                        .orElse(0.0);
                alerts.add(createAlert(recent.get(0), AlertType.ACCURACY_DROP,
                        "정답률이 전주 대비 %.1f%%p 하락한 상태가 3일간 지속되었습니다. 의료 진단이 아니며, 컨디션과 환경 변화를 확인해주세요."
                                .formatted((previousAccuracy - recentAccuracy) * 100),
                        recipients));
                return alerts.stream().map(CognitiveAlertResult::from).toList();
            }
            if (lastThreeDays.size() == 3
                    && previousResponse > 0
                    && lastThreeDays.stream().allMatch(
                    metric -> metric.participated()
                            && metric.getAverageResponseSeconds() >= previousResponse * 1.5)) {
                double recentResponse = lastThreeDays.stream()
                        .mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds)
                        .average()
                        .orElse(0.0);
                alerts.add(createAlert(recent.get(0), AlertType.RESPONSE_TIME_INCREASE,
                        "평균 반응 시간이 전주 대비 %.1f%% 증가한 상태가 3일간 지속되었습니다. 의료 진단이 아니며, 최근 생활 변화를 확인해주세요."
                                .formatted(((recentResponse / previousResponse) - 1.0) * 100),
                        recipients));
            }
        }

        return alerts.stream().map(CognitiveAlertResult::from).toList();
    }

    // F4-03: 기관 관리자 포털 조회
    @Transactional(readOnly = true)
    public InstitutionDashboardResult getInstitutionDashboard(GetInstitutionDashboardQuery query) {
        List<CognitiveDailyMetric> metrics = metricRepository.findByInstitutionIdAndDateBetween(
                query.institutionId(), query.from(), query.to());
        if (query.elderIds() != null && !query.elderIds().isEmpty()) {
            metrics = metrics.stream()
                    .filter(m -> query.elderIds().contains(m.getElderId()))
                    .toList();
        }
        if (metrics.isEmpty()) {
            throw new InstitutionSeniorsNotFoundException(query.institutionId());
        }
        double institutionAccuracy = metrics.stream()
                .mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate)
                .average().orElse(0.0);
        double institutionResponse = metrics.stream()
                .mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds)
                .average().orElse(0.0);

        Map<String, List<CognitiveDailyMetric>> byElder = metrics.stream()
                .collect(Collectors.groupingBy(CognitiveDailyMetric::getElderId, LinkedHashMap::new, Collectors.toList()));
        List<InstitutionDashboardResult.SeniorSummary> seniors = byElder.entrySet().stream()
                .map(entry -> {
                    List<CognitiveDailyMetric> values = entry.getValue();
                    double avgAccuracy = values.stream().mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate).average().orElse(0.0);
                    long periodDays = ChronoUnit.DAYS.between(query.from(), query.to()) + 1;
                    long participatedDays = values.stream()
                            .filter(CognitiveDailyMetric::participated)
                            .map(CognitiveDailyMetric::getMetricDate)
                            .distinct()
                            .count();
                    LocalDate currentWeekStart = query.to().minusDays(6);
                    List<CognitiveDailyMetric> currentWeek = values.stream()
                            .filter(value -> !value.getMetricDate().isBefore(currentWeekStart))
                            .toList();
                    List<CognitiveDailyMetric> previousWeek = metricRepository
                            .findByElderIdAndDateBetween(
                                    entry.getKey(),
                                    currentWeekStart.minusDays(7),
                                    currentWeekStart.minusDays(1)
                            );
                    double currentWeekAccuracy = currentWeek.stream()
                            .mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate)
                            .average()
                            .orElse(avgAccuracy);
                    double previousWeekAccuracy = previousWeek.stream()
                            .mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate)
                            .average()
                            .orElse(currentWeekAccuracy);
                    return new InstitutionDashboardResult.SeniorSummary(
                            anonymize(entry.getKey()),
                            entry.getKey(),
                            values.stream().mapToInt(CognitiveDailyMetric::getTrainingSessionCount).sum(),
                            participatedDays / (double) periodDays,
                            avgAccuracy,
                            values.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0),
                            currentWeekAccuracy - previousWeekAccuracy,
                            avgAccuracy - institutionAccuracy
                    );
                })
                .toList();

        return new InstitutionDashboardResult(
                query.institutionId(), query.from(), query.to(),
                institutionAccuracy, institutionResponse, seniors);
    }

    @Transactional(readOnly = true)
    public InstitutionDashboardExportResult exportInstitutionDashboard(
            GetInstitutionDashboardQuery query,
            DashboardExportFormat format
    ) {
        return institutionDashboardExportPort.export(
                getInstitutionDashboard(query), format);
    }

    private CognitiveChangeAlert createAlert(CognitiveDailyMetric metric, AlertType type,
                                             String message, AlertRecipientSetting recipients) {
        CognitiveChangeAlert alert = CognitiveChangeAlert.create(
                metric.getElderId(), metric.getAlbumId(), type, message + " 대응 가이드: " + GUIDE_LINK, GUIDE_LINK);
        CognitiveChangeAlert saved = alertRepository.save(alert);
        notificationPort.sendToGroup(
                recipients.recipientMemberIds(), "인지 변화 알림", saved.getMessage());
        return saved;
    }

    private AlertRecipientSetting loadAlertRecipients(String elderId) {
        return alertRecipientRepository.findByElderId(elderId)
                .orElseThrow(() -> new AlertRecipientsNotConfiguredException(elderId));
    }

    private LocalDate resolvePeriodEnd(ReportPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == ReportPeriod.WEEKLY) {
            return today.minusDays(1);
        }
        return today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

    private String buildChangeSummary(LocalDate start, LocalDate end,
                                      double avgAccuracy, double avgResponse,
                                      double accuracyChange, double responseChange,
                                      boolean noPreviousData) {
        if (noPreviousData) {
            return "%s ~ %s 평균 정답률 %.1f%%, 평균 반응 시간 %.1f초입니다. 비교할 이전 기간 데이터가 없습니다."
                    .formatted(start, end, avgAccuracy * 100, avgResponse);
        }
        return "%s ~ %s 평균 정답률 %.1f%%(%+.1f%%p), 평균 반응 시간 %.1f초(%+.1f초)입니다."
                .formatted(start, end, avgAccuracy * 100, accuracyChange * 100,
                        avgResponse, responseChange);
    }

    private UUID resolveAlbumId(GenerateCognitiveReportCommand command,
                                List<CognitiveDailyMetric> metrics) {
        if (command.albumId() != null && !command.albumId().isBlank()) {
            return UUID.fromString(command.albumId());
        }
        return metrics.stream()
                .map(CognitiveDailyMetric::getAlbumId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void sendReportNotification(CognitiveReport report) {
        Set<String> recipients = new LinkedHashSet<>();
        if (report.getAlbumId() != null) {
            albumRepository.findById(AlbumId.of(report.getAlbumId()))
                    .map(Album::getMemberIds)
                    .ifPresent(recipients::addAll);
        } else {
            albumRepository.findAllByElderProfileId(report.getElderId()).stream()
                    .map(Album::getMemberIds)
                    .forEach(recipients::addAll);
        }
        String body = report.getChangeSummary();
        if (report.getDeliveryMethod() == ReportDeliveryMethod.EMAIL
                || report.getDeliveryMethod() == ReportDeliveryMethod.IN_APP_AND_EMAIL) {
            body = "이메일 발송 연동 전까지 앱 내 알림으로 제공됩니다. " + body;
        }
        if (recipients.isEmpty()) {
            log.warn("인지 리포트 알림 대상 없음: reportId={}, elderId={}",
                    report.getId(), report.getElderId());
            return;
        }
        notificationPort.sendToGroup(recipients, "인지 리포트가 생성되었습니다", body);
    }

    private CognitiveReport loadReport(String reportId) {
        try {
            return reportRepository.findById(UUID.fromString(reportId))
                    .orElseThrow(() -> new CognitiveReportNotFoundException(reportId));
        } catch (IllegalArgumentException invalidId) {
            throw new CognitiveReportNotFoundException(reportId);
        }
    }

    private String anonymize(String elderId) {
        return "senior-" + Integer.toHexString(elderId.hashCode()).replace("-", "");
    }
}
