package com.memeboo2.haemi.m0.infrastructure.care;

import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FcmDeviceLockAdapterTest {

    @Test
    void lockSendsMemorialCommandToTheElderDevice() {
        PushDispatchService push = mock(PushDispatchService.class);
        UUID elderId = UUID.randomUUID();
        when(push.dispatchDeviceCommandToElder(eq(elderId.toString()), any()))
                .thenReturn(new PushSendResult(1, 0, java.util.List.of()));

        new FcmDeviceLockAdapter(push).lock(elderId);

        ArgumentCaptor<PushMessage> message = ArgumentCaptor.forClass(PushMessage.class);
        verify(push).dispatchDeviceCommandToElder(eq(elderId.toString()), message.capture());
        assertThat(message.getValue().data()).containsEntry("type", "DEVICE_COMMAND")
                .containsEntry("command", "LOCK_AND_OPEN_MEMORIAL");
    }

    @Test
    void missingDeliveryConfirmationFailsSoTheOutboxRetries() {
        PushDispatchService push = mock(PushDispatchService.class);
        UUID elderId = UUID.randomUUID();
        when(push.dispatchDeviceCommandToElder(eq(elderId.toString()), any())).thenReturn(PushSendResult.empty());

        assertThatThrownBy(() -> new FcmDeviceLockAdapter(push).unlock(elderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("활성 FCM 토큰");
    }
}
