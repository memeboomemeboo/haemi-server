package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import com.memeboo2.haemi.m0.application.service.ElderJoinApplicationService;
import com.memeboo2.haemi.m0.domain.event.ElderBereavementRecoveredEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ElderBereavementRecoveryListenerTest {
    @Test
    void recoveryCancelsPendingDeviceCommandsAndEnqueuesUnlock() {
        DeviceCommandDispatchService commands = mock(DeviceCommandDispatchService.class);
        ElderJoinApplicationService elderJoin = mock(ElderJoinApplicationService.class);
        UUID elderId = UUID.randomUUID();

        new ElderBereavementRecoveryListener(commands, elderJoin).onRecovered(
                new ElderBereavementRecoveredEvent(elderId, UUID.randomUUID(), LocalDateTime.now()));

        verify(commands).cancelPendingForRecoveredElder(elderId);
        verify(commands).enqueueBereavementRecoveryUnlock(elderId);
        verify(elderJoin).restoreAfterBereavementRecovery(eq(elderId), any());
    }
}
