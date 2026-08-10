package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;

import java.time.LocalDateTime;

public record DeviceTokenResult(
        String token,
        DevicePlatform platform,
        LocalDateTime registeredAt,
        LocalDateTime lastUsedAt
) {
    public static DeviceTokenResult from(DeviceToken deviceToken) {
        return new DeviceTokenResult(
                deviceToken.getToken(),
                deviceToken.getPlatform(),
                deviceToken.getRegisteredAt(),
                deviceToken.getLastUsedAt()
        );
    }
}
