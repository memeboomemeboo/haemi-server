package com.memeboo2.haemi.m0.infrastructure.care;

import com.memeboo2.haemi.m0.domain.port.ScheduledJobCancelPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 로그 기반 예약 잡 취소 어댑터 (개발/테스트 전용). 운영에서는 스케줄러/큐 취소 구현으로 교체한다.
 */
@Slf4j
@Component
public class LogScheduledJobCancelAdapter implements ScheduledJobCancelPort {

    @Override
    public void cancelAllForElder(UUID elderId) {
        log.info("[JOB-CANCEL] elderId={} 예약 잡 일괄 취소", elderId);
    }
}
