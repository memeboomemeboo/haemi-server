package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportMode;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** 가족·기관에 제공하는 v3.0 안전 리포트 DTO. 평가 점수와 개인화 레벨은 포함하지 않는다. */
public record ReminiscenceReportResult(
        String reportId,
        String elderId,
        ReportPeriod period,
        ReportMode mode,
        LocalDate periodStart,
        LocalDate periodEnd,
        int daysTogether,
        List<String> rememberedTopics,
        List<String> topDwelledPhotos,
        int voiceResponseCount,
        int familyContributionCount,
        String activityMessage,
        String summary,
        String medicalDisclaimer,
        String pdfKey,
        LocalDateTime createdAt
) {
    public static final String MEDICAL_DISCLAIMER = "이 자료는 앱 사용 기록을 정리한 것으로 의료적 진단이 아닙니다.";

    public static ReminiscenceReportResult from(CognitiveReport report) {
        return new ReminiscenceReportResult(
                report.getId().toString(), report.getElderId(), report.getPeriod(), report.getReportMode(),
                report.getPeriodStart(), report.getPeriodEnd(), report.getDaysTogether(),
                split(report.getRememberedTopics()), split(report.getTopDwelledPhotos()),
                report.getVoiceResponseCount(), report.getFamilyContributionCount(), report.getActivityMessage(),
                report.getChangeSummary(), MEDICAL_DISCLAIMER, report.getPdfKey(), report.getCreatedAt());
    }

    private static List<String> split(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }
        return Arrays.stream(source.split("\\|"))
                .filter(value -> !value.isBlank())
                .toList();
    }
}
