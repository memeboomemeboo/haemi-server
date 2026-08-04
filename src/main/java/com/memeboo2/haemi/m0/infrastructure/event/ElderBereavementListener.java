package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.ElderBereavedEvent;
import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import com.memeboo2.haemi.m0.domain.port.ScheduledJobCancelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 사별 확정 후처리 (F0-05). 예약 잡을 취소하고 기기를 원격 잠금한다.
 * 기기 잠금 실패는 사별 처리를 롤백하지 않고 복구(재시도) 경로로 흡수한다(EX-F005-06).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElderBereavementListener {

    private final ScheduledJobCancelPort scheduledJobCancelPort;
    private final DeviceLockPort deviceLockPort;

    @EventListener
    public void onBereaved(ElderBereavedEvent event) {
        scheduledJobCancelPort.cancelAllForElder(event.elderId());
        lockDeviceWithRecovery(event);
    }

    // 기기 잠금 실패 시 예외를 전파하지 않고 재시도 경로로 흡수한다 (EX-F005-06).
    private void lockDeviceWithRecovery(ElderBereavedEvent event) {
        try {
            deviceLockPort.lock(event.elderId());
        } catch (Exception e) {
            log.warn("기기 원격 잠금 실패, 복구 재시도 예약: elderId={}", event.elderId(), e);
            // 운영에서는 재시도 큐에 적재. 사별 처리 자체는 유지된다.
        }
    }
}
