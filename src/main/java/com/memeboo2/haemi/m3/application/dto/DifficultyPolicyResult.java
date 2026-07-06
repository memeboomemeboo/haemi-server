package com.memeboo2.haemi.m3.application.dto;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;
import com.memeboo2.haemi.m3.domain.model.training.QuestionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public record DifficultyPolicyResult(
        int level,
        double maxAverageResponseSeconds,
        double increaseAccuracyThreshold,
        double decreaseAccuracyThreshold,
        Set<QuestionType> questionTypes,
        LocalDateTime reviewedAt,
        String reviewedBy,
        LocalDate nextReviewDate
) {
    public static DifficultyPolicyResult from(DifficultyPolicy policy) {
        return new DifficultyPolicyResult(
                policy.getLevel(),
                policy.getMaxAverageResponseSeconds(),
                policy.getIncreaseAccuracyThreshold(),
                policy.getDecreaseAccuracyThreshold(),
                policy.getQuestionTypes(),
                policy.getReviewedAt(),
                policy.getReviewedBy(),
                policy.getNextReviewDate()
        );
    }
}
