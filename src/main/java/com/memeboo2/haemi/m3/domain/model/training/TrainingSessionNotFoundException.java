package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingSessionNotFoundException extends RuntimeException {
    public TrainingSessionNotFoundException(String sessionId) {
        super("인지 훈련 세션을 찾을 수 없습니다: " + sessionId);
    }
}
