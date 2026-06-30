package com.memeboo2.haemi.m5.domain.model.care;

public class AlarmTimeDuplicatedException extends RuntimeException {
    public AlarmTimeDuplicatedException() {
        super("이미 설정된 알람이 있습니다. 5분 간격으로 조정해주세요.");
    }
}
