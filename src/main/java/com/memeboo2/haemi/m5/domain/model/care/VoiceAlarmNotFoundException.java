package com.memeboo2.haemi.m5.domain.model.care;

public class VoiceAlarmNotFoundException extends RuntimeException {
    public VoiceAlarmNotFoundException(String alarmId) {
        super("손주 목소리 알람을 찾을 수 없습니다: " + alarmId);
    }
}
