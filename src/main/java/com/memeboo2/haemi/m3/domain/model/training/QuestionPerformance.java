package com.memeboo2.haemi.m3.domain.model.training;

public record QuestionPerformance(
        String questionId,
        String patternKey,
        QuestionType questionType,
        boolean correct,
        boolean timeout
) {
}
