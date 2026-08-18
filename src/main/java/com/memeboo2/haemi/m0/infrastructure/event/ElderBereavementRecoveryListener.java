package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import com.memeboo2.haemi.m0.application.service.ElderJoinApplicationService;
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
    private final ElderJoinApplicationService elderJoin;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRecovered(ElderBereavementRecoveredEvent event) {
        deviceCommands.cancelPendingForRecoveredElder(event.elderId());
        deviceCommands.enqueueBereavementRecoveryUnlock(event.elderId());
        // 오등록이었으므로 어르신에게 재로그인을 요구하지 않는다(F0-01-E 검증 지표: 재로그인 요구 0건).
        elderJoin.restoreAfterBereavementRecovery(event.elderId(), event.recoveredAt().minusHours(48));
    }
}
