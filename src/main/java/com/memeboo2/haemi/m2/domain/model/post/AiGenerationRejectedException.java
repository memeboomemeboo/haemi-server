package com.memeboo2.haemi.m2.domain.model.post;

/** 업스트림 AI가 애플리케이션이 보낸 요청을 거절한 경우. */
public class AiGenerationRejectedException extends RuntimeException {

    public AiGenerationRejectedException(String message) {
        super(message);
    }
}
