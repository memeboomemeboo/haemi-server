package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;

import java.time.LocalDate;

public record CognitiveMetricResult(
        String elderId,
        String albumId,
        String institutionId,
        LocalDate metricDate,
        int trainingSessionCount,
        double trainingAccuracyRate,
        double averageResponseSeconds,
        int reminiscenceReactionCount,
        int memoryPostCount,
        String mostReactedPhotoType
) {
    public static CognitiveMetricResult from(CognitiveDailyMetric metric) {
        return new CognitiveMetricResult(
                metric.getElderId(),
                metric.getAlbumId() != null ? metric.getAlbumId().toString() : null,
                metric.getInstitutionId(),
                metric.getMetricDate(),
                metric.getTrainingSessionCount(),
                metric.getTrainingAccuracyRate(),
                metric.getAverageResponseSeconds(),
                metric.getReminiscenceReactionCount(),
                metric.getMemoryPostCount(),
                metric.getMostReactedPhotoType()
        );
    }
}
