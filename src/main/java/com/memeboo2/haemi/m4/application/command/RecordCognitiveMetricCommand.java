package com.memeboo2.haemi.m4.application.command;

import java.time.LocalDate;

public record RecordCognitiveMetricCommand(
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
) {}
