package com.memeboo2.haemi.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FCM 발송 대상 기기 토큰 (#80). 토큰 자체가 식별자다.
 * 기기 재로그인처럼 같은 토큰이 다른 계정으로 다시 올라오면 소유자를 이전한다.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id
    @Column(name = "token", length = 255)
    private String token;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    public static DeviceToken register(String token, String memberId, DevicePlatform platform, LocalDateTime now) {
        DeviceToken deviceToken = new DeviceToken();
        deviceToken.token = token;
        deviceToken.memberId = memberId;
        deviceToken.platform = platform;
        deviceToken.registeredAt = now;
        deviceToken.lastUsedAt = now;
        return deviceToken;
    }

    // 같은 토큰 재등록: 소유자가 바뀌었으면 이전하고, 사용 시각을 갱신한다.
    public void refresh(String memberId, DevicePlatform platform, LocalDateTime now) {
        this.memberId = memberId;
        this.platform = platform;
        this.lastUsedAt = now;
    }

    public boolean isOwnedBy(String memberId) {
        return this.memberId.equals(memberId);
    }
}
