package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.command.RecordCognitiveMetricCommand;
import com.memeboo2.haemi.m4.application.dto.*;
import com.memeboo2.haemi.m4.application.query.GetCognitiveMetricQuery;
import com.memeboo2.haemi.m4.application.query.GetInstitutionDashboardQuery;
import com.memeboo2.haemi.m4.domain.model.dashboard.*;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.memeboo2.haemi.m4.domain.repository.CognitiveChangeAlertRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveMetricRepository;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
    private final PdfReportPort pdfReportPort;
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

        String summary = buildChangeSummary(command.elderId(), start, end, avgAccuracy, avgResponse);
        CognitiveReport report = CognitiveReport.create(
                command.elderId(),
                command.albumId() != null ? UUID.fromString(command.albumId()) : null,
                command.period(), start, end, participationCount, avgAccuracy,
                avgResponse, memoryPosts, summary, null);
        String pdfKey = pdfReportPort.generatePdf(report);
        CognitiveReport saved = reportRepository.save(CognitiveReport.create(
                command.elderId(),
                command.albumId() != null ? UUID.fromString(command.albumId()) : null,
                command.period(), start, end, participationCount, avgAccuracy,
                avgResponse, memoryPosts, summary, pdfKey));

        notificationPort.sendToMember(command.elderId(), "인지 리포트가 생성되었습니다", summary);
        return CognitiveReportResult.from(saved);
    }

    // F4-02: 조기 알림 트리거 검사
    @Transactional
    public List<CognitiveAlertResult> detectEarlyAlerts(String elderId) {
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
                    "최근 7일 동안 세션 참여가 없습니다. 의료 진단이 아니며, 생활 패턴 변화를 함께 확인해주세요."));
        }

        List<CognitiveDailyMetric> previous = metricRepository.findByElderIdAndDateBetween(
                elderId, today.minusDays(13), today.minusDays(7));
        if (previous.size() >= 7) {
            double recentAccuracy = recent.stream().mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate).average().orElse(0.0);
            double previousAccuracy = previous.stream().mapToDouble(CognitiveDailyMetric::getTrainingAccuracyRate).average().orElse(0.0);
            double recentResponse = recent.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0);
            double previousResponse = previous.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0);

            if (previousAccuracy > 0 && previousAccuracy - recentAccuracy >= 0.20) {
                alerts.add(createAlert(recent.get(0), AlertType.ACCURACY_DROP,
                        "정답률이 전주 대비 20% 이상 하락했습니다. 의료 진단이 아니며, 컨디션과 환경 변화를 확인해주세요."));
            }
            if (previousResponse > 0 && recentResponse >= previousResponse * 1.5) {
                alerts.add(createAlert(recent.get(0), AlertType.RESPONSE_TIME_INCREASE,
                        "평균 반응 시간이 전주 대비 50% 이상 증가했습니다. 의료 진단이 아니며, 최근 생활 변화를 확인해주세요."));
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
                    return new InstitutionDashboardResult.SeniorSummary(
                            anonymize(entry.getKey()),
                            entry.getKey(),
                            values.stream().mapToInt(CognitiveDailyMetric::getTrainingSessionCount).sum(),
                            avgAccuracy,
                            values.stream().mapToDouble(CognitiveDailyMetric::getAverageResponseSeconds).average().orElse(0.0),
                            avgAccuracy - institutionAccuracy
                    );
                })
                .toList();

        return new InstitutionDashboardResult(
                query.institutionId(), query.from(), query.to(),
                institutionAccuracy, institutionResponse, seniors);
    }

    private CognitiveChangeAlert createAlert(CognitiveDailyMetric metric, AlertType type, String message) {
        CognitiveChangeAlert alert = CognitiveChangeAlert.create(
                metric.getElderId(), metric.getAlbumId(), type, message + " 대응 가이드: " + GUIDE_LINK, GUIDE_LINK);
        CognitiveChangeAlert saved = alertRepository.save(alert);
        notificationPort.sendToMember(metric.getElderId(), "인지 변화 알림", saved.getMessage());
        return saved;
    }

    private LocalDate resolvePeriodEnd(ReportPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == ReportPeriod.WEEKLY) {
            return today.minusDays(1);
        }
        return today.minusDays(1);
    }

    private String buildChangeSummary(String elderId, LocalDate start, LocalDate end,
                                      double avgAccuracy, double avgResponse) {
        return "%s ~ %s 평균 정답률 %.1f%%, 평균 반응 시간 %.1f초입니다."
                .formatted(start, end, avgAccuracy * 100, avgResponse);
    }

    private String anonymize(String elderId) {
        return "senior-" + Integer.toHexString(elderId.hashCode()).replace("-", "");
    }
}
