package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        String changeSummary,
        String pdfKey,
        LocalDateTime createdAt
) {
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
                report.getChangeSummary(),
                report.getPdfKey(),
                report.getCreatedAt()
        );
    }
}
