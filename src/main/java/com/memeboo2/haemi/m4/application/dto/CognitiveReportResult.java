package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CognitiveReportResult(
        String reportId,
        String elderId,
        String albumId,
        ReportPeriod period,
        LocalDate periodStart,
        LocalDate periodEnd,
        int participationCount,
        double averageAccuracyRate,
        double averageResponseSeconds,
        int memoryPostCount,
        int reminiscenceParticipationCount,
        String mostReactedPhotoType,
        double accuracyChangeFromPrevious,
        double responseTimeChangeFromPrevious,
        List<TrendPointResult> accuracyTrend,
        String changeSummary,
        com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod deliveryMethod,
        String pdfKey,
        LocalDateTime viewedAt,
        LocalDateTime createdAt
) {
    public record TrendPointResult(LocalDate date, double accuracyRate) {}

    public static CognitiveReportResult from(CognitiveReport report) {
        return new CognitiveReportResult(
                report.getId().toString(),
                report.getElderId(),
                report.getAlbumId() != null ? report.getAlbumId().toString() : null,
                report.getPeriod(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                report.getParticipationCount(),
                report.getAverageAccuracyRate(),
                report.getAverageResponseSeconds(),
                report.getMemoryPostCount(),
                report.getReminiscenceParticipationCount(),
                report.getMostReactedPhotoType(),
                report.getAccuracyChangeFromPrevious(),
                report.getResponseTimeChangeFromPrevious(),
                report.getAccuracyTrend().stream()
                        .map(point -> new TrendPointResult(point.getDate(), point.getAccuracyRate()))
                        .toList(),
                report.getChangeSummary(),
                report.getDeliveryMethod(),
                report.getPdfKey(),
                report.getViewedAt(),
                report.getCreatedAt()
        );
    }
}
