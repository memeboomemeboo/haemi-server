package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import com.memeboo2.haemi.m0.domain.event.ElderBereavementRecoveredEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ElderBereavementRecoveryListenerTest {
    @Test
    void recoveryCancelsPendingDeviceCommands() {
        DeviceCommandDispatchService commands = mock(DeviceCommandDispatchService.class);
        UUID elderId = UUID.randomUUID();

        new ElderBereavementRecoveryListener(commands).onRecovered(
                new ElderBereavementRecoveredEvent(elderId, UUID.randomUUID(), LocalDateTime.now()));

        verify(commands).cancelPendingForRecoveredElder(elderId);
    }
}
