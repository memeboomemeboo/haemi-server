package com.memeboo2.haemi.m3.domain.model.training;

public class TrainingQuestionGenerationException extends RuntimeException {

    public TrainingQuestionGenerationException(Throwable cause) {
        super("인지 훈련 문제를 생성할 수 없습니다. 잠시 후 다시 시도해주세요.", cause);
    }
}
