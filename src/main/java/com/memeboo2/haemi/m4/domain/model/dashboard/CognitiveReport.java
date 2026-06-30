package com.memeboo2.haemi.m4.domain.model.dashboard;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Column(name = "pdf_key")
    private String pdfKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CognitiveReport create(String elderId, UUID albumId, ReportPeriod period,
                                         LocalDate periodStart, LocalDate periodEnd,
                                         int participationCount, double averageAccuracyRate,
                                         double averageResponseSeconds, int memoryPostCount,
                                         String changeSummary, String pdfKey) {
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
        report.changeSummary = changeSummary;
        report.pdfKey = pdfKey;
        report.createdAt = LocalDateTime.now();
        return report;
    }
}
