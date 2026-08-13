package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import com.memeboo2.haemi.m0.domain.event.ElderBereavementRecoveredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 오등록 복구 뒤에는 미전달 잠금 명령을 취소하고, 전달된 잠금을 되돌리는 명령을 발행한다. */
@Component
@RequiredArgsConstructor
public class ElderBereavementRecoveryListener {
    private final DeviceCommandDispatchService deviceCommands;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecovered(ElderBereavementRecoveredEvent event) {
        deviceCommands.cancelPendingForRecoveredElder(event.elderId());
        deviceCommands.enqueueBereavementRecoveryUnlock(event.elderId());
    }
}
