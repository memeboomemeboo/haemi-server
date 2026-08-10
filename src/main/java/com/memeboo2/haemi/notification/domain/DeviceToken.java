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
import java.util.UUID;

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

    /**
     * 이 토큰이 설치된 어르신 본인 휴대전화의 프로필 ID. 보호자 계정으로 대행 실행하는
     * Mode B에서도 어르신 기기 알림을 정확히 라우팅하기 위해 계정 소유자와 분리한다.
     */
    @Column(name = "elder_id", columnDefinition = "uuid")
    private UUID elderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    public static DeviceToken register(String token, String memberId, DevicePlatform platform, LocalDateTime now) {
        return register(token, memberId, platform, null, now);
    }

    public static DeviceToken register(String token, String memberId, DevicePlatform platform, UUID elderId,
                                       LocalDateTime now) {
        DeviceToken deviceToken = new DeviceToken();
        deviceToken.token = token;
        deviceToken.memberId = memberId;
        deviceToken.elderId = elderId;
        deviceToken.platform = platform;
        deviceToken.registeredAt = now;
        deviceToken.lastUsedAt = now;
        return deviceToken;
    }

    // 같은 토큰 재등록: 소유자가 바뀌었으면 이전하고, 사용 시각을 갱신한다.
    public void refresh(String memberId, DevicePlatform platform, LocalDateTime now) {
        refresh(memberId, platform, null, now);
    }

    public void refresh(String memberId, DevicePlatform platform, UUID elderId, LocalDateTime now) {
        this.memberId = memberId;
        this.elderId = elderId;
        this.platform = platform;
        this.lastUsedAt = now;
    }

    public boolean isOwnedBy(String memberId) {
        return this.memberId.equals(memberId);
    }
}
