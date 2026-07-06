package com.memeboo2.haemi.m3.domain.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DifficultyLevelChangedEvent(
        UUID sessionId,
        String elderId,
        UUID albumId,
        int previousLevel,
        int currentLevel,
        double threeSessionMovingAverage,
        List<String> repeatedWrongQuestionIds,
        LocalDateTime changedAt
) {
}
