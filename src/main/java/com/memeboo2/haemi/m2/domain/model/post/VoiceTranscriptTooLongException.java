package com.memeboo2.haemi.m2.domain.model.post;

/** 음성 전사 결과가 어르신 답변 저장 계약을 넘긴 경우. */
public class VoiceTranscriptTooLongException extends RuntimeException {

    public VoiceTranscriptTooLongException(int maxLength) {
        super("말씀하신 내용이 너무 길어요. %d자 이하로 짧게 다시 녹음해주세요.".formatted(maxLength));
    }
}
