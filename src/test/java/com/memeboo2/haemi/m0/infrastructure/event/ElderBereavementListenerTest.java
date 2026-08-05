package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.ElderBereavedEvent;
import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import com.memeboo2.haemi.m0.domain.port.ScheduledJobCancelPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElderBereavementListenerTest {

    @Mock ScheduledJobCancelPort scheduledJobCancelPort;
    @Mock DeviceLockPort deviceLockPort;
    @InjectMocks ElderBereavementListener listener;

    private ElderBereavedEvent event() {
        UUID elderId = UUID.randomUUID();
        return new ElderBereavedEvent(elderId, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("사별 시 예약 잡을 취소하고 기기를 잠근다")
    void onBereaved_cancelsJobsAndLocks() {
        ElderBereavedEvent event = event();

        listener.onBereaved(event);

        verify(scheduledJobCancelPort).cancelAllForElder(event.elderId());
        verify(deviceLockPort).lock(event.elderId());
    }

    @Test
    @DisplayName("EX-F005-06: 기기 잠금이 실패해도 사별 처리는 롤백되지 않고 복구 경로로 흡수된다")
    void ex_f005_06_deviceLockFailureIsRecovered() {
        ElderBereavedEvent event = event();
        doThrow(new RuntimeException("MDM timeout")).when(deviceLockPort).lock(event.elderId());

        assertThatCode(() -> listener.onBereaved(event)).doesNotThrowAnyException();

        verify(scheduledJobCancelPort).cancelAllForElder(event.elderId());
        verify(deviceLockPort).lock(event.elderId());
    }
}
