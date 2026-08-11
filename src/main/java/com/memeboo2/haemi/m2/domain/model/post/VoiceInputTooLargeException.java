package com.memeboo2.haemi.m2.domain.model.post;

public class VoiceInputTooLargeException extends RuntimeException {

    public VoiceInputTooLargeException(long maxBytes) {
        super("음성 답변이 너무 커요. %dMB 이하로 짧게 다시 녹음해주세요."
                .formatted(maxBytes / (1024 * 1024)));
    }
}
