package com.memeboo2.haemi.m2.domain.model.post;

public class UnsupportedVoiceContentTypeException extends RuntimeException {

    public UnsupportedVoiceContentTypeException() {
        super("음성 답변은 audio 형식 파일로 보내주세요.");
    }
}
