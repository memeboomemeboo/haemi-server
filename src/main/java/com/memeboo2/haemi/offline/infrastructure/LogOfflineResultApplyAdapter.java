package com.memeboo2.haemi.offline.infrastructure;

import com.memeboo2.haemi.offline.domain.OfflineSessionResult;
import com.memeboo2.haemi.offline.domain.port.OfflineResultApplyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그 기반 오프라인 결과 반영 어댑터 (개발/테스트 전용).
 * 운영에서는 m3 난이도 프로필·리포트 반영 구현으로 교체한다.
 */
@Slf4j
@Component
public class LogOfflineResultApplyAdapter implements OfflineResultApplyPort {

    @Override
    public void apply(OfflineSessionResult result) {
        log.info("[OFFLINE-APPLY] elderId={} sessionId={} responded={} noResponse={} completedAt={}",
                result.elderId(), result.sessionId(),
                result.respondedCount(), result.noResponseCount(), result.completedAt());
    }
}
