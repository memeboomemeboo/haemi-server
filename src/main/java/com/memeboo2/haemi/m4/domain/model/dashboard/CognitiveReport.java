package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cognitive_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CognitiveReport {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "album_id", columnDefinition = "uuid")
    private UUID albumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private ReportPeriod period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "participation_count", nullable = false)
    private int participationCount;

    @Column(name = "average_accuracy_rate", nullable = false)
    private double averageAccuracyRate;

    @Column(name = "average_response_seconds", nullable = false)
    private double averageResponseSeconds;

    @Column(name = "memory_post_count", nullable = false)
    private int memoryPostCount;

    @Column(name = "reminiscence_participation_count", nullable = false)
    private int reminiscenceParticipationCount;

    @Column(name = "most_reacted_photo_type")
    private String mostReactedPhotoType;

    @Column(name = "accuracy_change_from_previous", nullable = false)
    private double accuracyChangeFromPrevious;

    @Column(name = "response_time_change_from_previous", nullable = false)
    private double responseTimeChangeFromPrevious;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cognitive_report_accuracy_trend",
            joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "trend_order")
    private List<ReportTrendPoint> accuracyTrend = new ArrayList<>();

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method", nullable = false)
    private ReportDeliveryMethod deliveryMethod;

    @Column(name = "pdf_key")
    private String pdfKey;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_mode", nullable = false)
    private ReportMode reportMode;

    @Column(name = "days_together", nullable = false)
    private int daysTogether;

    @Column(name = "remembered_topics", length = 1000)
    private String rememberedTopics;

    @Column(name = "top_dwelled_photos", length = 1000)
    private String topDwelledPhotos;

    @Column(name = "voice_response_count", nullable = false)
    private int voiceResponseCount;

    @Column(name = "family_contribution_count", nullable = false)
    private int familyContributionCount;

    @Column(name = "activity_message", length = 1000)
    private String activityMessage;

    public static CognitiveReport create(String elderId, UUID albumId, ReportPeriod period,
                                         LocalDate periodStart, LocalDate periodEnd,
                                         int participationCount, double averageAccuracyRate,
                                         double averageResponseSeconds, int memoryPostCount,
                                         int reminiscenceParticipationCount,
                                         String mostReactedPhotoType,
                                         double accuracyChangeFromPrevious,
                                         double responseTimeChangeFromPrevious,
                                         List<ReportTrendPoint> accuracyTrend,
                                         String changeSummary,
                                         ReportDeliveryMethod deliveryMethod) {
        CognitiveReport report = new CognitiveReport();
        report.id = UUID.randomUUID();
        report.elderId = elderId;
        report.albumId = albumId;
        report.period = period;
        report.periodStart = periodStart;
        report.periodEnd = periodEnd;
        report.participationCount = participationCount;
        report.averageAccuracyRate = averageAccuracyRate;
        report.averageResponseSeconds = averageResponseSeconds;
        report.memoryPostCount = memoryPostCount;
        report.reminiscenceParticipationCount = reminiscenceParticipationCount;
        report.mostReactedPhotoType = mostReactedPhotoType;
        report.accuracyChangeFromPrevious = accuracyChangeFromPrevious;
        report.responseTimeChangeFromPrevious = responseTimeChangeFromPrevious;
        report.accuracyTrend = new ArrayList<>(accuracyTrend);
        report.changeSummary = changeSummary;
        report.deliveryMethod = deliveryMethod;
        report.createdAt = LocalDateTime.now();
        report.reportMode = ReportMode.STANDARD;
        report.daysTogether = participationCount;
        report.voiceResponseCount = 0;
        report.familyContributionCount = memoryPostCount;
        return report;
    }

    public static CognitiveReport createReminiscence(String elderId, UUID albumId, ReportPeriod period,
                                                      LocalDate periodStart, LocalDate periodEnd, ReportMode reportMode,
                                                      int daysTogether, List<String> rememberedTopics,
                                                      List<String> topDwelledPhotos, int voiceResponseCount,
                                                      int familyContributionCount, String activityMessage,
                                                      String summary, ReportDeliveryMethod deliveryMethod) {
        CognitiveReport report = create(elderId, albumId, period, periodStart, periodEnd,
                daysTogether, 0.0, 0.0, familyContributionCount, voiceResponseCount,
                null, 0.0, 0.0, List.of(), summary, deliveryMethod);
        report.reportMode = reportMode;
        report.daysTogether = Math.max(daysTogether, 0);
        report.rememberedTopics = join(rememberedTopics);
        report.topDwelledPhotos = join(topDwelledPhotos);
        report.voiceResponseCount = Math.max(voiceResponseCount, 0);
        report.familyContributionCount = Math.max(familyContributionCount, 0);
        report.activityMessage = activityMessage;
        return report;
    }

    public void assignPdfKey(String pdfKey) {
        if (pdfKey == null || pdfKey.isBlank()) {
            throw new IllegalArgumentException("PDF 경로는 비어 있을 수 없습니다.");
        }
        this.pdfKey = pdfKey;
    }

    public void markViewed(LocalDateTime viewedAt) {
        if (this.viewedAt == null) {
            this.viewedAt = viewedAt;
        }
    }

    public List<ReportTrendPoint> getAccuracyTrend() {
        return Collections.unmodifiableList(accuracyTrend);
    }

    public void changeToMemoryFocused() {
        reportMode = ReportMode.MEMORY_FOCUSED;
        activityMessage = null;
    }

    private static String join(List<String> values) {
        return values == null ? null : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .reduce((left, right) -> left + "|" + right)
                .orElse(null);
    }
}
