package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushSenderConfigTest {

    private final PushSenderConfig config = new PushSenderConfig();

    @SuppressWarnings("unchecked")
    private ObjectProvider<FirebaseMessaging> provider(FirebaseMessaging messaging) {
        ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(messaging);
        return provider;
    }

    private MockEnvironment environment(String profile) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        return environment;
    }

    @Test
    @DisplayName("FCM 자격증명이 없으면 로그 폴백으로 뜬다")
    void fallsBackToLoggingWithoutFirebase() {
        PushSenderPort sender = config.pushSenderPort(provider(null), environment("test"));

        assertThat(sender).isInstanceOf(LoggingPushSenderAdapter.class);
    }

    @Test
    @DisplayName("FirebaseMessaging 빈이 있으면 FCM 어댑터로 뜬다")
    void usesFcmWhenFirebaseAvailable() {
        PushSenderPort sender = config.pushSenderPort(provider(mock(FirebaseMessaging.class)), environment("prod"));

        assertThat(sender).isInstanceOf(FcmPushSenderAdapter.class);
    }

    @Test
    @DisplayName("운영 프로필에서 자격증명이 없으면 기동을 거부한다")
    void prodFailsWithoutFirebase() {
        assertThatThrownBy(() -> config.pushSenderPort(provider(null), environment("prod")))
                .isInstanceOf(IllegalStateException.class);
    }
}
