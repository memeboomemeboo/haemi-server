package com.memeboo2.haemi.m4.application.dto;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveDailyMetric;

import java.time.LocalDate;

public record ReminiscenceMetricResult(String elderId, LocalDate metricDate, int sessionCount,
                                       int voiceDetectedCount, double averageDwellMs,
                                       int hintPlaybackCount, int hintNoResponseCount,
                                       int familyContributionCount, String topMemoryTopic,
                                       String topDwelledPhoto) {
    public static ReminiscenceMetricResult from(CognitiveDailyMetric metric) {
        return new ReminiscenceMetricResult(metric.getElderId(), metric.getMetricDate(),
                metric.getTrainingSessionCount(), metric.getVoiceDetectedCount(), metric.getAverageDwellMs(),
                metric.getHintPlaybackCount(), metric.getHintNoResponseCount(), metric.getMemoryPostCount(),
                metric.getTopMemoryTopic(), metric.getTopDwelledPhoto());
    }
}
