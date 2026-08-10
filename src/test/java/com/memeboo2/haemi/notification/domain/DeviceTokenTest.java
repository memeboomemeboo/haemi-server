package com.memeboo2.haemi.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTokenTest {

    private static final LocalDateTime REGISTERED_AT = LocalDateTime.of(2026, 8, 10, 9, 0);

    @Test
    @DisplayName("등록 시 소유자와 시각이 함께 기록된다")
    void register() {
        DeviceToken token = DeviceToken.register("tok-1", "member-1", DevicePlatform.ANDROID, REGISTERED_AT);

        assertThat(token.getToken()).isEqualTo("tok-1");
        assertThat(token.getMemberId()).isEqualTo("member-1");
        assertThat(token.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(token.getRegisteredAt()).isEqualTo(REGISTERED_AT);
        assertThat(token.getLastUsedAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @DisplayName("기기 재로그인으로 같은 토큰이 다시 올라오면 소유자를 이전한다")
    void refreshTransfersOwnership() {
        DeviceToken token = DeviceToken.register("tok-1", "member-1", DevicePlatform.ANDROID, REGISTERED_AT);
        LocalDateTime later = REGISTERED_AT.plusDays(3);

        token.refresh("member-2", DevicePlatform.IOS, later);

        assertThat(token.getMemberId()).isEqualTo("member-2");
        assertThat(token.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(token.getLastUsedAt()).isEqualTo(later);
        // 최초 등록 시각은 보존한다.
        assertThat(token.getRegisteredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @DisplayName("소유자 판별은 등록한 사용자에게만 참이다")
    void isOwnedBy() {
        DeviceToken token = DeviceToken.register("tok-1", "member-1", DevicePlatform.WEB, REGISTERED_AT);

        assertThat(token.isOwnedBy("member-1")).isTrue();
        assertThat(token.isOwnedBy("member-2")).isFalse();
    }
}
