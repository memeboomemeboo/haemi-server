package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingSessionAlreadyCompletedException extends RuntimeException {
    public TrainingSessionAlreadyCompletedException() {
        super("이미 완료된 훈련 세션입니다.");
    }
}
