package com.memeboo2.haemi.m4.application.dto;

import java.time.LocalDate;
import java.util.List;

public record InstitutionDashboardResult(
        String institutionId,
        LocalDate from,
        LocalDate to,
        double institutionAverageAccuracyRate,
        double institutionAverageResponseSeconds,
        List<SeniorSummary> seniors
) {
    public record SeniorSummary(
            String anonymizedSeniorId,
            String elderId,
            int participationCount,
            double participationRate,
            double averageAccuracyRate,
            double averageResponseSeconds,
            double weeklyAccuracyChange,
            double accuracyDeltaFromInstitution
    ) {}
}
