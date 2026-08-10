package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.m0.domain.port.ElderStatusQuery;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    @Mock ElderStatusQuery elderStatusQuery;

    PushDispatchService service;

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PushDispatchService(deviceTokens, pushSender, elderStatusQuery);
    }

    private DeviceToken token(String token, UUID memberId) {
        return DeviceToken.register(token, memberId, DevicePlatform.ANDROID, LocalDateTime.now());
    }

    @Test
    @DisplayName("수신자의 모든 기기 토큰으로 발송한다")
    void dispatchesToAllTokensOfMembers() {
        when(deviceTokens.findByMemberIds(any()))
                .thenReturn(List.of(token("tok-1", MEMBER_1), token("tok-2", MEMBER_1),
                        token("tok-3", MEMBER_2)));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(3, 0, List.of()));

        service.dispatchToMembers(Set.of(MEMBER_1.toString(), MEMBER_2.toString()), PushMessage.of("제목", "본문"));

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.captor();
        verify(pushSender).send(tokens.capture(), any(PushMessage.class));
        assertThat(tokens.getValue()).containsExactlyInAnyOrder("tok-1", "tok-2", "tok-3");
    }

    @Test
    @DisplayName("회원 ID 형식이 아닌 수신자가 섞여도 나머지 수신자에게는 발송한다")
    void skipsMalformedRecipientsWithoutLosingTheRest() {
        // M4 기관 담당자 ID처럼 UUID가 아닌 값이 수신자 목록에 섞일 수 있다.
        when(deviceTokens.findByMemberIds(List.of(MEMBER_1)))
                .thenReturn(List.of(token("tok-1", MEMBER_1)));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(1, 0, List.of()));

        service.dispatchToMembers(
                new LinkedHashSet<>(List.of(MEMBER_1.toString(), "manager-api-test")),
                PushMessage.of("제목", "본문"));

        ArgumentCaptor<List<String>> tokens = ArgumentCaptor.captor();
        verify(pushSender).send(tokens.capture(), any(PushMessage.class));
        assertThat(tokens.getValue()).containsExactly("tok-1");
    }

    @Test
    @DisplayName("수신자가 전부 회원 ID 형식이 아니면 저장소를 조회하지 않는다")
    void doesNotQueryWhenEveryRecipientIsMalformed() {
        service.dispatchToMembers(Set.of("manager-api-test", "elder-device"), PushMessage.of("제목", "본문"));

        verify(deviceTokens, never()).findByMemberIds(any());
        verify(pushSender, never()).send(anyList(), any());
    }

    @Test
    @DisplayName("등록된 토큰이 없으면 발송을 시도하지 않는다")
    void skipsWhenNoTokens() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of());

        service.dispatchToMember(MEMBER_1.toString(), PushMessage.of("제목", "본문"));

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
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", MEMBER_1)));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(0, 1, List.of("tok-1")));

        service.dispatchToMember(MEMBER_1.toString(), PushMessage.of("제목", "본문"));

        ArgumentCaptor<Collection<String>> deleted = ArgumentCaptor.captor();
        verify(deviceTokens).deleteAllByTokens(deleted.capture());
        assertThat(deleted.getValue()).containsExactly("tok-1");
    }

    @Test
    @DisplayName("일시 실패만 있으면 토큰을 지우지 않는다")
    void keepsTokensOnTransientFailure() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", MEMBER_1)));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(0, 1, List.of()));

        service.dispatchToMember(MEMBER_1.toString(), PushMessage.of("제목", "본문"));

        verify(deviceTokens, never()).deleteAllByTokens(any());
    }

    @Test
    @DisplayName("발송이 예외로 실패해도 호출자에게 전파하지 않는다")
    void swallowsSendFailure() {
        when(deviceTokens.findByMemberIds(any())).thenReturn(List.of(token("tok-1", MEMBER_1)));
        when(pushSender.send(anyList(), any())).thenThrow(new RuntimeException("FCM 장애"));

        assertThatCode(() -> service.dispatchToMember(MEMBER_1.toString(), PushMessage.of("제목", "본문")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("어르신 알림은 계정 소유 토큰이 아니라 연결된 어르신 기기 토큰으로만 발송한다")
    void dispatchesOnlyToBoundElderDevice() {
        String elderId = UUID.randomUUID().toString();
        when(elderStatusQuery.isDispatchable(elderId)).thenReturn(true);
        when(deviceTokens.findByElderId(UUID.fromString(elderId))).thenReturn(List.of(
                DeviceToken.register("elder-device", UUID.randomUUID(), DevicePlatform.ANDROID,
                        UUID.fromString(elderId), LocalDateTime.now())));
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(1, 0, List.of()));

        service.dispatchToElder(elderId, PushMessage.of("제목", "본문"));

        verify(pushSender).send(List.of("elder-device"), PushMessage.of("제목", "본문"));
        verify(deviceTokens, never()).findByMemberIds(any());
    }
}
