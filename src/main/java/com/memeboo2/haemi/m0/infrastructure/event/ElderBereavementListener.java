package com.memeboo2.haemi.m0.infrastructure.event;

import com.memeboo2.haemi.m0.domain.event.ElderBereavedEvent;
import com.memeboo2.haemi.m0.application.service.DeviceCommandDispatchService;
import com.memeboo2.haemi.m0.application.service.ElderJoinApplicationService;
import com.memeboo2.haemi.m0.domain.model.ElderSessionRevokeReason;
import com.memeboo2.haemi.m0.domain.port.ScheduledJobCancelPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
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
    private final DeviceCommandDispatchService deviceCommands;
    private final ElderJoinApplicationService elderJoin;

    /** 사별 상태가 실제 커밋된 뒤에만 예약 취소와 기기 잠금 명령을 발행한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBereaved(ElderBereavedEvent event) {
        // 기기 잠금 명령이 도달하기 전이라도 서버가 먼저 세션을 끊어 어르신 화면을 닫는다(EX-F005-06).
        try {
            elderJoin.revokeAll(event.elderId(), ElderSessionRevokeReason.ELDER_STATUS_CHANGED);
        } catch (Exception e) {
            log.error("사별 후 어르신 세션 폐기 실패: elderId={}", event.elderId(), e);
        }
        try {
            scheduledJobCancelPort.cancelAllForElder(event.elderId());
        } catch (Exception e) {
            log.warn("사별 후 예약 취소 실패: elderId={}", event.elderId(), e);
        }
        // 실패해도 DB 아웃박스에 남아 스케줄러가 잠금/기억보관함 전환을 재시도한다.
        try {
            deviceCommands.enqueueBereavementLock(event.elderId());
        } catch (Exception e) {
            // DB 장애는 운영 경보 대상이지만, 이미 확정한 사별 상태를 롤백하지 않는다.
            log.error("사별 기기 명령 아웃박스 적재 실패: elderId={}", event.elderId(), e);
        }
    }
}
