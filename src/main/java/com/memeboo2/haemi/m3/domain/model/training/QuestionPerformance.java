package com.memeboo2.haemi.m3.domain.model.training;

public record QuestionPerformance(
        String questionId,
        QuestionType questionType,
        boolean responded,
        boolean timeout
) {
}
