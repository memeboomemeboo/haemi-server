package com.memeboo2.haemi.m3.infrastructure.event;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import com.memeboo2.haemi.m3.domain.event.HintRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

class TrainingEventListenerTest {

    @Test
    @DisplayName("손주 찬스 요청 이벤트는 실제 가족 구성원에게 알림을 보낸다")
    void onHintRequested_sendsNotificationToRecipients() {
        NotificationPort notificationPort = mock(NotificationPort.class);
        TrainingEventListener listener = new TrainingEventListener(notificationPort);
        Set<String> recipients = Set.of("family-1", "family-2");

        listener.onHintRequested(new HintRequestedEvent(
                UUID.randomUUID(),
                "elder-1",
                UUID.randomUUID(),
                "q1",
                1,
                recipients,
                LocalDateTime.now()
        ));

        verify(notificationPort).sendToGroup(
                eq(recipients),
                eq("어르신이 힌트를 요청했습니다"),
                contains("힌트")
        );
    }

    @Test
    @DisplayName("손주 찬스 요청 대상이 없으면 알림을 보내지 않는다")
    void onHintRequested_skipsEmptyRecipients() {
        NotificationPort notificationPort = mock(NotificationPort.class);
        TrainingEventListener listener = new TrainingEventListener(notificationPort);

        listener.onHintRequested(new HintRequestedEvent(
                UUID.randomUUID(),
                "elder-1",
                UUID.randomUUID(),
                "q1",
                1,
                Set.of(),
                LocalDateTime.now()
        ));

        verifyNoInteractions(notificationPort);
    }
}
