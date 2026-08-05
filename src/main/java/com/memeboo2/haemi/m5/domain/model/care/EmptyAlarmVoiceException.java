package com.memeboo2.haemi.m5.domain.model.care;

public class EmptyAlarmVoiceException extends RuntimeException {
    public EmptyAlarmVoiceException() {
        super("추가할 음성 파일이 필요합니다.");
    }
}
