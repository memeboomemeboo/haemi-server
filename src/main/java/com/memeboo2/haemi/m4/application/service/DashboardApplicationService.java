package com.memeboo2.haemi.m4.application.service;

import com.memeboo2.haemi.common.exception.DomainValidationException;
import com.memeboo2.haemi.common.support.DomainIds;
import com.memeboo2.haemi.m1.domain.model.album.Album;
import com.memeboo2.haemi.m1.domain.model.album.AlbumId;
import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m1.domain.repository.AlbumRepository;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import com.memeboo2.haemi.m0.domain.model.M0NotFoundException;
import com.memeboo2.haemi.m0.domain.port.ElderAccessPort;
import com.memeboo2.haemi.m4.application.command.GenerateCognitiveReportCommand;
import com.memeboo2.haemi.m4.application.command.RecordReminiscenceMetricCommand;
import com.memeboo2.haemi.m4.application.command.RecordCognitiveMetricCommand;
import com.memeboo2.haemi.m4.application.command.UpdateAlertRecipientsCommand;
import com.memeboo2.haemi.m4.application.dto.*;
import com.memeboo2.haemi.m4.application.query.GetCognitiveMetricQuery;
import com.memeboo2.haemi.m4.domain.model.dashboard.*;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
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
    private final NotificationPort notificationPort;
    private final ElderAccessPort elderAccess;
    private final ActivityChangeLanguagePolicy activityLanguage;
    private final MemberRepository members;

    @Transactional
    public CognitiveMetricResult recordMetric(RecordCognitiveMetricCommand command) {
        LocalDate date = command.metricDate() != null ? command.metricDate() : LocalDate.now();
        CognitiveDailyMetric metric = metricRepository.findByElderIdAndMetricDate(command.elderId(), date)
                .orElseGet(() -> CognitiveDailyMetric.create(
                        command.elderId(),
                        command.albumId() != null ? DomainIds.parseUuid(command.albumId(), "앨범 ID") : null,
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

    /** F4-01/F4-02가 쓰는 v3 회상 기록 집계 입력. */
    @Transactional
    public ReminiscenceMetricResult recordReminiscenceMetric(RecordReminiscenceMetricCommand command) {
        LocalDate date = command.metricDate() == null ? LocalDate.now() : command.metricDate();
        CognitiveDailyMetric metric = metricRepository.findByElderIdAndMetricDate(command.elderId(), date)
                .orElseGet(() -> CognitiveDailyMetric.create(command.elderId(), parseAlbumId(command.albumId()),
                        command.institutionId(), date, 0, 0.0, 0.0, 0, 0, null));
        metric.updateReminiscenceSnapshot(command.institutionId(), command.sessionCount(), command.voiceDetectedCount(),
                command.averageDwellMs(), command.hintPlaybackCount(), command.hintNoResponseCount(),
                command.familyContributionCount(), command.topMemoryTopic(), command.topDwelledPhoto());
        return ReminiscenceMetricResult.from(metricRepository.save(metric));
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

    @Transactional(readOnly = true)
    public List<ReminiscenceMetricResult> getReminiscenceMetrics(GetCognitiveMetricQuery query) {
        List<CognitiveDailyMetric> metrics = metricRepository.findByElderIdAndDateBetween(
                query.elderId(), query.from(), query.to());
        if (metrics.isEmpty()) {
            throw new CognitiveMetricNotFoundException(query.elderId());
        }
        return metrics.stream().map(ReminiscenceMetricResult::from).toList();
    }

    // F4-01: 주간·월간 리포트 생성
    @Transactional
    public ReminiscenceReportResult generateReport(GenerateCognitiveReportCommand command) {
        ElderAccessPort.ElderAccessSnapshot elder = loadElder(command.elderId());
        if (elder.status() == ElderStatus.DECEASED) {
            throw new ReportDeliveryBlockedException();
        }
        LocalDate end = resolvePeriodEnd(command.period());
        LocalDate start = command.period() == ReportPeriod.WEEKLY
                ? end.minusDays(6)
                : end.with(TemporalAdjusters.firstDayOfMonth());

        List<CognitiveDailyMetric> metrics = metricRepository.findByElderIdAndDateBetween(
                command.elderId(), start, end);
        if (metrics.size() < 7) {
            throw new DataInsufficientException();
        }

        ReportMode mode = elder.status() == ElderStatus.DECLINING
                ? ReportMode.MEMORY_FOCUSED : ReportMode.STANDARD;
        int daysTogether = (int) metrics.stream()
                .filter(CognitiveDailyMetric::participated)
                .map(CognitiveDailyMetric::getMetricDate)
                .distinct()
                .count();
        if (daysTogether == 0) {
            throw new DataInsufficientException("이번 기간에는 회상 기록이 없어 리포트를 보내지 않아요.");
        }
        List<String> topics = topValues(metrics, CognitiveDailyMetric::getTopMemoryTopic);
        List<String> topPhotos = topValues(metrics, CognitiveDailyMetric::getTopDwelledPhoto);
        int voiceResponses = metrics.stream().mapToInt(CognitiveDailyMetric::getVoiceDetectedCount).sum();
        int familyContributions = metrics.stream().mapToInt(CognitiveDailyMetric::getMemoryPostCount).sum();
        List<CognitiveDailyMetric> previousMetrics = mode == ReportMode.STANDARD
                ? previousPeriodMetrics(command.elderId(), start, end) : List.of();
        String activityMessage = mode == ReportMode.STANDARD
                ? buildActivityMessage(daysTogether, previousMetrics) : null;
        String summary = buildReminiscenceSummary(mode, daysTogether, topics, voiceResponses, familyContributions);
        activityLanguage.requireSafe(summary);
        if (activityMessage != null) {
            activityLanguage.requireSafe(activityMessage);
        }
        UUID albumId = resolveAlbumId(command, metrics);
        CognitiveReport report = CognitiveReport.createReminiscence(command.elderId(), albumId, command.period(),
                start, end, mode, daysTogether, topics, topPhotos, voiceResponses, familyContributions,
                activityMessage, summary, command.deliveryMethod());
        String pdfKey = pdfReportPort.generatePdf(report);
        report.assignPdfKey(pdfKey);

        // 발송 직전 상태를 다시 읽어 사별 후 발송과 declining 상태의 standard 리포트를 막는다.
        ElderAccessPort.ElderAccessSnapshot finalElder = loadElder(command.elderId());
        if (finalElder.status() == ElderStatus.DECEASED) {
            throw new ReportDeliveryBlockedException();
        }
        if (finalElder.status() == ElderStatus.DECLINING && report.getReportMode() != ReportMode.MEMORY_FOCUSED) {
            report.changeToMemoryFocused();
            report.assignPdfKey(pdfReportPort.generatePdf(report));
        }
        CognitiveReport saved = reportRepository.save(report);

        sendReportNotification(saved);
        return ReminiscenceReportResult.from(saved);
    }

    @Transactional
    public ReminiscenceReportResult markReportViewed(String reportId) {
        CognitiveReport report = loadReport(reportId);
        report.markViewed(LocalDateTime.now());
        return ReminiscenceReportResult.from(reportRepository.save(report));
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
                validatedInstitutionManagerIds(command.institutionManagerMemberIds())
        );
        return AlertRecipientSettingResult.from(alertRecipientRepository.save(setting));
    }

    @Transactional(readOnly = true)
    public AlertRecipientSettingResult getAlertRecipients(String elderId) {
        return AlertRecipientSettingResult.from(loadAlertRecipients(elderId));
    }

    @Transactional
    public CognitiveAlertResult markAlertFalsePositive(String alertId) {
        CognitiveChangeAlert alert = alertRepository.findById(parseAlertId(alertId))
                .orElseThrow(() -> new M0NotFoundException("활동 변화 안내"));
        alert.markFalsePositive(LocalDateTime.now());
        return CognitiveAlertResult.from(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public CognitiveChangeAlert getAlert(String alertId) {
        return alertRepository.findById(parseAlertId(alertId))
                .orElseThrow(() -> new M0NotFoundException("활동 변화 안내"));
    }

    @Transactional
    public List<CognitiveAlertResult> detectEarlyAlerts(String elderId) {
        ElderAccessPort.ElderAccessSnapshot elder = loadElder(elderId);
        if (!elder.isElderFacingDeliveryAllowed()) {
            return List.of();
        }
        AlertRecipientSetting recipients = loadAlertRecipients(elderId);
        // Stage 2 검증 전에는 기관 담당자에게만 활동 안내를 보낸다.
        Set<String> deliveryRecipients = recipients.getInstitutionManagerMemberIds();
        if (deliveryRecipients.isEmpty()) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        List<CognitiveDailyMetric> history = metricRepository.findByElderIdAndDateBetween(
                elderId, today.minusDays(13), today).stream()
                .sorted(Comparator.comparing(CognitiveDailyMetric::getMetricDate))
                .toList();
        if (history.size() < 14) {
            throw new DataInsufficientException();
        }
        if (alertRepository.findLatestByElderIdSince(elderId, LocalDateTime.now().minusDays(7)).isPresent()) {
            return List.of();
        }

        List<CognitiveDailyMetric> previousWeek = history.subList(0, 7);
        List<CognitiveDailyMetric> currentWeek = history.subList(7, 14);
        List<CognitiveDailyMetric> lastThreeDays = currentWeek.subList(4, 7);
        CognitiveDailyMetric representative = currentWeek.getFirst();
        boolean lowerSensitivity = alertRepository.existsFalsePositiveByElderIdSince(
                elderId, LocalDateTime.now().minusDays(28));

        if (currentWeek.subList(2, 7).stream().noneMatch(CognitiveDailyMetric::participated)) {
            return List.of(CognitiveAlertResult.from(createAlert(representative, AlertType.NO_REMINISCENCE_5_DAYS,
                    "최근 5일 동안 회상을 열어보신 기록이 없어요. 전화 한 번 드려보시는 건 어떠실까요?",
                    deliveryRecipients)));
        }

        double previousVoiceRate = voiceRate(previousWeek);
        if (previousVoiceRate > 0 && lastThreeDays.stream()
                .allMatch(metric -> dailyVoiceRate(metric) <= previousVoiceRate * (lowerSensitivity ? 0.4 : 0.6))) {
            return List.of(CognitiveAlertResult.from(createAlert(representative, AlertType.VOICE_ACTIVITY_DROP,
                    "최근 며칠 사이 목소리가 담긴 회상 기록이 줄었어요. 컨디션과 환경을 함께 살펴봐 주세요.",
                    deliveryRecipients)));
        }

        double previousDwell = previousWeek.stream().mapToDouble(CognitiveDailyMetric::getAverageDwellMs)
                .filter(value -> value > 0).average().orElse(0.0);
        if (previousDwell > 0 && lastThreeDays.stream()
                .allMatch(metric -> metric.getAverageDwellMs() > 0
                        && metric.getAverageDwellMs() <= previousDwell * (lowerSensitivity ? 0.3 : 0.5))) {
            return List.of(CognitiveAlertResult.from(createAlert(representative, AlertType.DWELL_TIME_DROP,
                    "요즘 사진을 함께 보는 시간이 줄었어요. 좋아하시는 사진으로 이야기를 시작해보셔도 좋아요.",
                    deliveryRecipients)));
        }

        if (lastThreeDays.stream().allMatch(metric -> metric.getHintPlaybackCount() > 0
                && metric.getHintNoResponseCount() / (double) metric.getHintPlaybackCount()
                >= (lowerSensitivity ? 0.9 : 0.8))) {
            return List.of(CognitiveAlertResult.from(createAlert(representative, AlertType.HINT_NO_RESPONSE_HIGH,
                    "한마디를 들으신 뒤에도 조용한 시간이 길었어요. 다음 회상에는 더 익숙한 사진을 골라보셔도 좋아요.",
                    deliveryRecipients)));
        }
        return List.of();
    }

    private CognitiveChangeAlert createAlert(CognitiveDailyMetric metric, AlertType type,
                                             String message, Set<String> recipients) {
        String safeMessage = message + " 이 안내는 앱 사용 기록에 기반한 것으로, 건강 상태에 대한 의학적 판단이 아닙니다.";
        activityLanguage.requireSafe(safeMessage);
        CognitiveChangeAlert alert = CognitiveChangeAlert.create(
                metric.getElderId(), metric.getAlbumId(), type, safeMessage + " 안내: " + GUIDE_LINK, GUIDE_LINK);
        CognitiveChangeAlert saved = alertRepository.save(alert);
        notificationPort.sendToGroup(recipients, "어르신 소식이에요", saved.getMessage());
        return saved;
    }

    private AlertRecipientSetting loadAlertRecipients(String elderId) {
        return alertRecipientRepository.findByElderId(elderId)
                .orElseThrow(() -> new AlertRecipientsNotConfiguredException(elderId));
    }

    private Set<String> validatedInstitutionManagerIds(Set<String> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) {
            return Set.of();
        }
        Set<String> validatedIds = new LinkedHashSet<>();
        for (String requestedId : requestedIds) {
            if (requestedId == null || requestedId.isBlank()) {
                continue;
            }
            UUID memberId = DomainIds.parseUuid(requestedId, "기관 담당자 회원 ID");
            members.findById(memberId)
                    .filter(member -> member.isActive() && member.getRole() == MemberRole.INSTITUTION_ADMIN)
                    .orElseThrow(() -> new DomainValidationException("활성 기관 관리자 계정만 알림 수신자로 등록할 수 있어요."));
            validatedIds.add(memberId.toString());
        }
        return validatedIds;
    }

    private LocalDate resolvePeriodEnd(ReportPeriod period) {
        LocalDate today = LocalDate.now();
        if (period == ReportPeriod.WEEKLY) {
            return today.minusDays(1);
        }
        return today.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
    }

    private List<CognitiveDailyMetric> previousPeriodMetrics(String elderId, LocalDate start, LocalDate end) {
        long periodDays = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate previousEnd = start.minusDays(1);
        return metricRepository.findByElderIdAndDateBetween(elderId,
                previousEnd.minusDays(periodDays - 1), previousEnd);
    }

    private List<String> topValues(List<CognitiveDailyMetric> metrics,
                                   java.util.function.Function<CognitiveDailyMetric, String> extractor) {
        return metrics.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String buildReminiscenceSummary(ReportMode mode, int daysTogether, List<String> topics,
                                            int voiceResponses, int familyContributions) {
        String topic = topics.isEmpty() ? "사진과 함께한 이야기" : String.join(", ", topics);
        if (mode == ReportMode.MEMORY_FOCUSED) {
            return "이번 기간에 함께한 %d일의 회상 기록을 모았어요. %s에 관한 이야기가 남아 있어요."
                    .formatted(daysTogether, topic);
        }
        return "이번 기간에 %d일 함께 회상했어요. %s 이야기가 자주 이어졌고, 가족이 남긴 기록은 %d건이에요. 목소리가 담긴 반응은 %d건이에요."
                .formatted(daysTogether, topic, familyContributions, voiceResponses);
    }

    private String buildActivityMessage(int currentDays, List<CognitiveDailyMetric> previousMetrics) {
        if (previousMetrics.isEmpty()) {
            return "이전 기간과 비교할 기록이 더 쌓이면 함께한 날의 변화를 알려드릴게요.";
        }
        int previousDays = (int) previousMetrics.stream().filter(CognitiveDailyMetric::participated)
                .map(CognitiveDailyMetric::getMetricDate).distinct().count();
        int difference = currentDays - previousDays;
        if (difference > 0) {
            return "지난 기간보다 함께한 날이 %d일 늘었어요.".formatted(difference);
        }
        if (difference < 0) {
            return "지난 기간보다 함께한 날이 %d일 줄었어요.".formatted(Math.abs(difference));
        }
        return "지난 기간과 함께한 날 수가 같아요.";
    }

    private double voiceRate(List<CognitiveDailyMetric> metrics) {
        int sessions = metrics.stream().mapToInt(CognitiveDailyMetric::getTrainingSessionCount).sum();
        return sessions == 0 ? 0.0
                : metrics.stream().mapToInt(CognitiveDailyMetric::getVoiceDetectedCount).sum() / (double) sessions;
    }

    private double dailyVoiceRate(CognitiveDailyMetric metric) {
        return metric.getTrainingSessionCount() == 0 ? 0.0
                : metric.getVoiceDetectedCount() / (double) metric.getTrainingSessionCount();
    }

    private ElderAccessPort.ElderAccessSnapshot loadElder(String elderId) {
        return elderAccess.getRequired(DomainIds.parseUuid(elderId, "어르신 ID"));
    }

    private UUID parseAlbumId(String albumId) {
        if (albumId == null || albumId.isBlank()) {
            return null;
        }
        return DomainIds.parseUuid(albumId, "앨범 ID");
    }

    private UUID resolveAlbumId(GenerateCognitiveReportCommand command,
                                List<CognitiveDailyMetric> metrics) {
        UUID supplied = parseAlbumId(command.albumId());
        if (supplied != null) return supplied;
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
        String body = report.getChangeSummary() + " " + ReminiscenceReportResult.MEDICAL_DISCLAIMER;
        if (report.getDeliveryMethod() == ReportDeliveryMethod.EMAIL
                || report.getDeliveryMethod() == ReportDeliveryMethod.IN_APP_AND_EMAIL) {
            body = "이메일 발송 연동 전까지 앱 내 알림으로 제공됩니다. " + body;
        }
        if (recipients.isEmpty()) {
            log.warn("회상 리포트 알림 대상 없음: reportId={}, elderId={}",
                    report.getId(), report.getElderId());
            return;
        }
        notificationPort.sendToGroup(recipients, "회상 기록이 준비되었어요", body);
    }

    private CognitiveReport loadReport(String reportId) {
        try {
            return reportRepository.findById(UUID.fromString(reportId))
                    .orElseThrow(() -> new CognitiveReportNotFoundException(reportId));
        } catch (IllegalArgumentException invalidId) {
            throw new CognitiveReportNotFoundException(reportId);
        }
    }

    private UUID parseAlertId(String alertId) {
        try {
            return UUID.fromString(alertId);
        } catch (IllegalArgumentException invalidId) {
            throw new M0NotFoundException("활동 변화 안내");
        }
    }

}
