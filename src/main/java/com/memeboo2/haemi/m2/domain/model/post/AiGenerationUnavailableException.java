package com.memeboo2.haemi.m2.domain.model.post;

/** 외부 생성형 AI가 설정되지 않았거나 응답하지 않아 결과를 신뢰할 수 없는 경우. */
public class AiGenerationUnavailableException extends RuntimeException {

    public AiGenerationUnavailableException(String message) {
        super(message);
    }

    public AiGenerationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
