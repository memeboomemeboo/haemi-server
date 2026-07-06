package com.memeboo2.haemi.m3.application.command;

import com.memeboo2.haemi.m3.domain.model.training.QuestionType;

import java.time.LocalDate;
import java.util.Set;

public record UpdateDifficultyPolicyCommand(
        int level,
        double maxAverageResponseSeconds,
        double increaseAccuracyThreshold,
        double decreaseAccuracyThreshold,
        Set<QuestionType> questionTypes,
        String reviewedBy,
        LocalDate reviewedDate
) {
}
