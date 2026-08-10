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

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

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

    public static DeviceToken register(String token, UUID memberId, DevicePlatform platform, LocalDateTime now) {
        return register(token, memberId, platform, null, now);
    }

    public static DeviceToken register(String token, UUID memberId, DevicePlatform platform, UUID elderId,
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
    public void refresh(UUID memberId, DevicePlatform platform, LocalDateTime now) {
        refresh(memberId, platform, null, now);
    }

    /**
     * 같은 소유자가 elderId 없이 재등록하면 기존 어르신 기기 연결을 그대로 둔다.
     * 앱이 토큰 갱신 콜백에서 elderId 없이 재등록하는 것만으로 어르신 알림 경로가
     * 조용히 끊기면 안 되기 때문이다. 연결 해제는 토큰 해지 후 재등록으로 한다.
     *
     * <p>다만 소유자가 바뀐 기기는 이전 어르신 연결을 이어받지 않는다.
     * 기기를 넘겨받은 사람에게 남의 어르신 알림이 계속 가면 안 된다.
     */
    public void refresh(UUID memberId, DevicePlatform platform, UUID elderId, LocalDateTime now) {
        boolean ownerChanged = !isOwnedBy(memberId);
        this.memberId = memberId;
        if (elderId != null) {
            this.elderId = elderId;
        } else if (ownerChanged) {
            this.elderId = null;
        }
        this.platform = platform;
        this.lastUsedAt = now;
    }

    public boolean isOwnedBy(UUID memberId) {
        return this.memberId.equals(memberId);
    }
}
