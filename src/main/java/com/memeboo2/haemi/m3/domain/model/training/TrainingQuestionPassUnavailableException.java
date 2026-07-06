package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingQuestionPassUnavailableException extends RuntimeException {

    public TrainingQuestionPassUnavailableException() {
        super("손주 찬스 요청 후 30분이 지나야 문제를 건너뛸 수 있습니다.");
    }
}
