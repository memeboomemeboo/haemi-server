package com.memeboo2.haemi.notification;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.notification.application.DeviceTokenService;
import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * 알림 발송이 실제 컨테이너에서 호출자 스레드 밖으로 넘어가는지, 무효 토큰 정리까지 이어지는지 검증한다 (#80).
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("test")
class NotificationAsyncDispatchIntegrationTest {

    @MockitoBean PushSenderPort pushSender;

    private final NotificationPort notificationPort;
    private final DeviceTokenService deviceTokenService;
    private final DeviceTokenRepository deviceTokens;

    @Autowired
    NotificationAsyncDispatchIntegrationTest(NotificationPort notificationPort,
                                             DeviceTokenService deviceTokenService,
                                             DeviceTokenRepository deviceTokens) {
        this.notificationPort = notificationPort;
        this.deviceTokenService = deviceTokenService;
        this.deviceTokens = deviceTokens;
    }

    @Test
    @DisplayName("발송은 호출자 스레드가 아니라 알림 전용 스레드에서 처리된다")
    void dispatchesOnNotificationExecutor() throws Exception {
        String memberId = UUID.randomUUID().toString();
        String token = "fcm-" + UUID.randomUUID();
        deviceTokenService.register(memberId, token, DevicePlatform.ANDROID);

        AtomicReference<String> senderThread = new AtomicReference<>();
        CountDownLatch sent = new CountDownLatch(1);
        when(pushSender.send(anyList(), any())).thenAnswer(invocation -> {
            senderThread.set(Thread.currentThread().getName());
            sent.countDown();
            return new PushSendResult(1, 0, List.of());
        });

        String callerThread = Thread.currentThread().getName();
        notificationPort.sendToGroup(Set.of(memberId), "제목", "본문");

        assertThat(sent.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(senderThread.get()).startsWith("push-").isNotEqualTo(callerThread);
    }

    @Test
    @DisplayName("FCM이 영구 실패로 답한 토큰은 저장소에서 정리된다")
    void prunesInvalidTokensAfterDispatch() {
        String memberId = UUID.randomUUID().toString();
        String token = "fcm-" + UUID.randomUUID();
        deviceTokenService.register(memberId, token, DevicePlatform.ANDROID);

        when(pushSender.send(anyList(), any()))
                .thenReturn(new PushSendResult(0, 1, List.of(token)));

        notificationPort.sendToMember(memberId, "제목", "본문");

        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(deviceTokens.findByToken(token)).isEmpty());
    }
}
