package com.memeboo2.haemi.m2.domain.model.post;

/** AI 제공자 또는 애플리케이션의 음성 전사 처리 용량이 일시적으로 부족한 경우. */
public class AiGenerationRateLimitedException extends RuntimeException {

    public AiGenerationRateLimitedException(String message) {
        super(message);
    }
}
