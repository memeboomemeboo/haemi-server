package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.DeviceCommand;
import com.memeboo2.haemi.m0.domain.model.DeviceCommandStatus;
import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import com.memeboo2.haemi.m0.domain.repository.DeviceCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandDispatchServiceTest {

    @Mock DeviceCommandRepository commands;
    @Mock DeviceLockPort deviceLockPort;
    @InjectMocks DeviceCommandDispatchService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAttempts", 10);
    }

    @Test
    void successfulDispatchIsPersistedAsDelivered() {
        when(commands.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueBereavementLock(UUID.randomUUID());

        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commands, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(DeviceCommandStatus.DELIVERED);
        assertThat(captor.getAllValues().getLast().getDeliveredAt()).isNotNull();
    }

    @Test
    void failedDispatchRemainsPendingForRetry() {
        UUID elderId = UUID.randomUUID();
        when(commands.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("MDM timeout")).when(deviceLockPort).lock(elderId);

        service.enqueueBereavementLock(elderId);

        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commands, org.mockito.Mockito.times(2)).save(captor.capture());
        DeviceCommand failed = captor.getAllValues().getLast();
        assertThat(failed.getStatus()).isEqualTo(DeviceCommandStatus.PENDING);
        assertThat(failed.getAttempts()).isEqualTo(1);
        assertThat(failed.getNextAttemptAt()).isAfter(failed.getCreatedAt());
    }

    @Test
    void retriesOnlyDuePendingCommands() {
        DeviceCommand due = DeviceCommand.lockAndOpenMemorial(UUID.randomUUID(), LocalDateTime.now().minusMinutes(1));
        when(commands.findPendingBefore(any())).thenReturn(List.of(due));

        int count = service.retryDueCommands(LocalDateTime.now());

        assertThat(count).isEqualTo(1);
        verify(deviceLockPort).lock(due.getElderId());
        assertThat(due.getStatus()).isEqualTo(DeviceCommandStatus.DELIVERED);
    }

    @Test
    void recoveryCancelsEveryPendingLockCommandForTheElder() {
        DeviceCommand first = DeviceCommand.lockAndOpenMemorial(UUID.randomUUID(), LocalDateTime.now());
        DeviceCommand second = DeviceCommand.lockAndOpenMemorial(first.getElderId(), LocalDateTime.now());
        when(commands.findPendingByElderId(first.getElderId())).thenReturn(List.of(first, second));

        int cancelled = service.cancelPendingForRecoveredElder(first.getElderId());

        assertThat(cancelled).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(DeviceCommandStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(DeviceCommandStatus.CANCELLED);
        verify(commands, org.mockito.Mockito.times(2)).save(any(DeviceCommand.class));
    }

    @Test
    void recoveryEnqueuesAndDispatchesAnUnlockCommand() {
        UUID elderId = UUID.randomUUID();
        when(commands.save(any(DeviceCommand.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueBereavementRecoveryUnlock(elderId);

        ArgumentCaptor<DeviceCommand> captor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(commands, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getAction())
                .isEqualTo(com.memeboo2.haemi.m0.domain.model.DeviceCommandAction.UNLOCK_AND_RESUME);
        assertThat(captor.getAllValues().getLast().getStatus()).isEqualTo(DeviceCommandStatus.DELIVERED);
        verify(deviceLockPort).unlock(elderId);
    }
}
