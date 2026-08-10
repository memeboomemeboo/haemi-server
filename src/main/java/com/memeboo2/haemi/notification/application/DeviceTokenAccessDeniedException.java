package com.memeboo2.haemi.notification.application;

public class DeviceTokenAccessDeniedException extends RuntimeException {

    public DeviceTokenAccessDeniedException() {
        super("본인 기기의 알림 토큰만 해지할 수 있어요.");
    }

    public DeviceTokenAccessDeniedException(String message) {
        super(message);
    }
}
