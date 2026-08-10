package com.memeboo2.haemi.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceTokenTest {

    private static final LocalDateTime REGISTERED_AT = LocalDateTime.of(2026, 8, 10, 9, 0);
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID OTHER_OWNER = UUID.randomUUID();

    @Test
    @DisplayName("등록 시 소유자와 시각이 함께 기록된다")
    void register() {
        DeviceToken token = DeviceToken.register("tok-1", OWNER, DevicePlatform.ANDROID, REGISTERED_AT);

        assertThat(token.getToken()).isEqualTo("tok-1");
        assertThat(token.getMemberId()).isEqualTo(OWNER);
        assertThat(token.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(token.getRegisteredAt()).isEqualTo(REGISTERED_AT);
        assertThat(token.getLastUsedAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @DisplayName("기기 재로그인으로 같은 토큰이 다시 올라오면 소유자를 이전한다")
    void refreshTransfersOwnership() {
        DeviceToken token = DeviceToken.register("tok-1", OWNER, DevicePlatform.ANDROID, REGISTERED_AT);
        LocalDateTime later = REGISTERED_AT.plusDays(3);

        token.refresh(OTHER_OWNER, DevicePlatform.IOS, later);

        assertThat(token.getMemberId()).isEqualTo(OTHER_OWNER);
        assertThat(token.getPlatform()).isEqualTo(DevicePlatform.IOS);
        assertThat(token.getLastUsedAt()).isEqualTo(later);
        // 최초 등록 시각은 보존한다.
        assertThat(token.getRegisteredAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    @DisplayName("같은 소유자가 elderId 없이 재등록해도 어르신 기기 연결은 유지된다")
    void refreshKeepsElderBindingWhenElderIdOmitted() {
        UUID elderId = UUID.randomUUID();
        DeviceToken token = DeviceToken.register("tok-1", OWNER, DevicePlatform.ANDROID, elderId, REGISTERED_AT);

        token.refresh(OWNER, DevicePlatform.ANDROID, REGISTERED_AT.plusDays(1));

        assertThat(token.getElderId()).isEqualTo(elderId);
    }

    @Test
    @DisplayName("소유자가 바뀐 기기는 이전 어르신 연결을 이어받지 않는다")
    void refreshDropsElderBindingWhenOwnerChanges() {
        UUID elderId = UUID.randomUUID();
        DeviceToken token = DeviceToken.register("tok-1", OWNER, DevicePlatform.ANDROID, elderId, REGISTERED_AT);

        token.refresh(OTHER_OWNER, DevicePlatform.ANDROID, REGISTERED_AT.plusDays(1));

        assertThat(token.getElderId()).isNull();
    }

    @Test
    @DisplayName("소유자 판별은 등록한 사용자에게만 참이다")
    void isOwnedBy() {
        DeviceToken token = DeviceToken.register("tok-1", OWNER, DevicePlatform.WEB, REGISTERED_AT);

        assertThat(token.isOwnedBy(OWNER)).isTrue();
        assertThat(token.isOwnedBy(OTHER_OWNER)).isFalse();
    }
}
