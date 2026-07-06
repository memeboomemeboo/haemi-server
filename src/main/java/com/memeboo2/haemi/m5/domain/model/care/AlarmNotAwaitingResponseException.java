package com.memeboo2.haemi.m5.domain.model.care;

public class AlarmNotAwaitingResponseException extends RuntimeException {

    public AlarmNotAwaitingResponseException() {
        super("현재 응답을 기다리는 알람이 없습니다.");
    }
}
