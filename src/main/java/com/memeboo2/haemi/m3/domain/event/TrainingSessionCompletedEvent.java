package com.memeboo2.haemi.m3.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TrainingSessionCompletedEvent(
        UUID sessionId,
        String elderId,
        UUID albumId,
        LocalDate sessionDate,
        double accuracyRate,
        double averageResponseSeconds,
        int completedQuestionCount,
        LocalDateTime completedAt
) {}
