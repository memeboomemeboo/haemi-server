package com.memeboo2.haemi.m1.domain.model.album;

public class SyncConditionNotMetException extends RuntimeException {
    public SyncConditionNotMetException(String message) {
        super(message);
    }

    public static SyncConditionNotMetException wifiRequired() {
        return new SyncConditionNotMetException("Wi-Fi 연결 시 재개됩니다.");
    }

    public static SyncConditionNotMetException lowBattery() {
        return new SyncConditionNotMetException("배터리 충전 후 재개됩니다.");
    }
}
