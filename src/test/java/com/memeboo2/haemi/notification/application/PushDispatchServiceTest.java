package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDispatchServiceTest {

    @Mock DeviceTokenRepository deviceTokens;
    @Mock PushSenderPort pushSender;

    PushDispatchService service;

    @BeforeEach
    void setUp() {
        service = new PushDispatchService(deviceTokens, pushSender);
    }

    private DeviceToken token(String token, String memberId) {
        return DeviceToken.register(token, memberId, DevicePlatform.ANDROID, LocalDateTime.now());
    }

    @Test
    @DisplayName("수신자의 모든 기기 토큰으로 발송한다")
    void dispatchesToAllTokensOfMembers() {
        when(deviceTokens.findByMemberIds(any()))
                .thenReturn(List.of(token("tok-1", "member-1"), token("tok-2", "member-1"),
                        token("tok-3", "member-2")));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(3, 0, List.of()));

        service.dispatchToMembers(Set.of("member-1", "member-2"), PushMessage.of("제목", "본문"));

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.captor();
        verify(pushSender).send(tokens.capture(), any(PushMessage.class));
        assertThat(tokens.getValue()).containsExactlyInAnyOrder("tok-1", "tok-2", "tok-3");
    }

    @Test
    @DisplayName("등록된 토큰이 없으면 발송을 시도하지 않는다")
    void skipsWhenNoTokens() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of());

        service.dispatchToMember("member-1", PushMessage.of("제목", "본문"));

        verify(pushSender, never()).send(anyList(), any());
    }

    @Test
    @DisplayName("수신자 목록이 비면 조회조차 하지 않는다")
    void skipsWhenNoRecipients() {
        service.dispatchToMembers(Set.of(), PushMessage.of("제목", "본문"));

        verify(deviceTokens, never()).findByMemberIds(any());
        verify(pushSender, never()).send(anyList(), any());
    }

    @Test
    @DisplayName("영구 실패로 응답한 토큰은 정리한다")
    void deletesInvalidTokens() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", "member-1")));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(0, 1, List.of("tok-1")));

        service.dispatchToMember("member-1", PushMessage.of("제목", "본문"));

        ArgumentCaptor<Collection<String>> deleted = ArgumentCaptor.captor();
        verify(deviceTokens).deleteAllByTokens(deleted.capture());
        assertThat(deleted.getValue()).containsExactly("tok-1");
    }

    @Test
    @DisplayName("일시 실패만 있으면 토큰을 지우지 않는다")
    void keepsTokensOnTransientFailure() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", "member-1")));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(0, 1, List.of()));

        service.dispatchToMember("member-1", PushMessage.of("제목", "본문"));

        verify(deviceTokens, never()).deleteAllByTokens(any());
    }

    @Test
    @DisplayName("발송이 예외로 실패해도 호출자에게 전파하지 않는다")
    void swallowsSendFailure() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", "member-1")));
        when(pushSender.send(anyList(), any())).thenThrow(new RuntimeException("FCM 장애"));

        assertThatCode(() -> service.dispatchToMember("member-1", PushMessage.of("제목", "본문")))
                .doesNotThrowAnyException();
    }
}
