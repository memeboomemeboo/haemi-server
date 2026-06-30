package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingQuestionNotFoundException extends RuntimeException {
    public TrainingQuestionNotFoundException(String questionId) {
        super("훈련 문제를 찾을 수 없습니다: " + questionId);
    }
}
