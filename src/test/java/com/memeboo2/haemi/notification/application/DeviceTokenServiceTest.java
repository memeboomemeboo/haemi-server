package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock DeviceTokenRepository deviceTokens;
    @Mock ElderDeviceAccessValidator elderDeviceAccessValidator;

    DeviceTokenService service;

    @BeforeEach
    void setUp() {
        service = new DeviceTokenService(deviceTokens, elderDeviceAccessValidator);
        lenientSave();
    }

    private static final UUID MEMBER = UUID.randomUUID();
    private static final UUID OTHER_MEMBER = UUID.randomUUID();

    private void lenientSave() {
        org.mockito.Mockito.lenient()
                .when(deviceTokens.save(any(DeviceToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("새 토큰은 인증 주체를 소유자로 저장된다")
    void registerNewToken() {
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.empty());

        DeviceTokenResult result = service.register(MEMBER, "tok-1", DevicePlatform.ANDROID);

        assertThat(result.token()).isEqualTo("tok-1");
        assertThat(result.platform()).isEqualTo(DevicePlatform.ANDROID);
        verify(deviceTokens).save(any(DeviceToken.class));
    }

    @Test
    @DisplayName("이미 있는 토큰을 다시 등록하면 새로 만들지 않고 소유자를 이전한다")
    void registerExistingTokenTransfersOwner() {
        DeviceToken existing = DeviceToken.register("tok-1", MEMBER, DevicePlatform.ANDROID,
                LocalDateTime.of(2026, 8, 1, 9, 0));
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.of(existing));

        service.register(OTHER_MEMBER, "tok-1", DevicePlatform.IOS);

        assertThat(existing.getMemberId()).isEqualTo(OTHER_MEMBER);
        assertThat(existing.getPlatform()).isEqualTo(DevicePlatform.IOS);
        verify(deviceTokens).save(existing);
    }

    @Test
    @DisplayName("어르신 본인 휴대전화 토큰은 계정 소유자와 별도로 어르신 프로필에 연결된다")
    void registerElderDeviceToken() {
        UUID memberId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.empty());

        DeviceTokenResult result = service.register(memberId, "tok-1", DevicePlatform.ANDROID, elderId);

        assertThat(result.elderId()).isEqualTo(elderId);
        verify(elderDeviceAccessValidator).requireCanBind(memberId, elderId);
    }

    @Test
    @DisplayName("본인 토큰은 해지된다")
    void unregisterOwnToken() {
        DeviceToken existing = DeviceToken.register("tok-1", MEMBER, DevicePlatform.ANDROID, LocalDateTime.now());
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.of(existing));

        service.unregister(MEMBER, "tok-1");

        verify(deviceTokens).deleteByToken("tok-1");
    }

    @Test
    @DisplayName("남의 토큰은 해지할 수 없다")
    void unregisterOtherMembersTokenIsRejected() {
        DeviceToken existing = DeviceToken.register("tok-1", MEMBER, DevicePlatform.ANDROID, LocalDateTime.now());
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.unregister(OTHER_MEMBER, "tok-1"))
                .isInstanceOf(DeviceTokenAccessDeniedException.class);

        verify(deviceTokens, never()).deleteByToken(any());
    }

    @Test
    @DisplayName("없는 토큰 해지는 조용히 성공한다 (로그아웃 재시도 멱등)")
    void unregisterMissingTokenIsIdempotent() {
        when(deviceTokens.findByToken("tok-1")).thenReturn(Optional.empty());

        service.unregister(MEMBER, "tok-1");

        verify(deviceTokens, never()).deleteByToken(any());
    }
}
