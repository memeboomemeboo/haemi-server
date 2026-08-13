package com.memeboo2.haemi.m0.infrastructure.care;

import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/** 운영에서는 FCM 데이터 메시지로 사별 기기 잠금과 오등록 복구를 전달한다. */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class FcmDeviceLockAdapter implements DeviceLockPort {

    private final PushDispatchService pushDispatch;

    @Override
    public void lock(UUID elderId) {
        dispatch(elderId, "LOCK_AND_OPEN_MEMORIAL", "기억 보관함으로 전환합니다.");
    }

    @Override
    public void unlock(UUID elderId) {
        dispatch(elderId, "UNLOCK_AND_RESUME", "서비스를 다시 이용할 수 있습니다.");
    }

    private void dispatch(UUID elderId, String command, String body) {
        PushSendResult result = pushDispatch.dispatchDeviceCommandToElder(elderId.toString(), new PushMessage(
                "해미 기기 명령", body, Map.of("type", "DEVICE_COMMAND", "command", command)));
        if (result.successCount() == 0) {
            throw new IllegalStateException("기기 명령을 수신할 활성 FCM 토큰이 없습니다.");
        }
    }
}
