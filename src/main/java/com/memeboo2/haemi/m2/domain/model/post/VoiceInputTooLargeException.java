package com.memeboo2.haemi.m2.domain.model.post;

public class VoiceInputTooLargeException extends RuntimeException {

    public VoiceInputTooLargeException(long maxBytes) {
        super("음성 답변은 %dMB 이하로 보내주세요.".formatted(maxBytes / (1024 * 1024)));
    }
}
