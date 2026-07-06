package com.memeboo2.haemi.m3.domain.model.training;

import java.util.List;

public record DifficultyAdjustment(
        int previousLevel,
        int currentLevel,
        double threeSessionMovingAverage,
        boolean extremeScoreBuffered,
        List<String> repeatedWrongQuestionIds
) {
    public boolean levelChanged() {
        return previousLevel != currentLevel;
    }
}
