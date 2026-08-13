package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingQuestionPassUnavailableException extends RuntimeException {

    public TrainingQuestionPassUnavailableException() {
        super("실시간 힌트 요청 후 60초가 지나야 문제를 건너뛸 수 있습니다.");
    }
}
