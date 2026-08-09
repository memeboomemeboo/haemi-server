package com.memeboo2.haemi.m0.infrastructure.care;

import com.memeboo2.haemi.m0.domain.port.DeviceLockPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 로그 기반 기기 원격 잠금 어댑터 (개발/테스트 전용). 운영에서는 MDM/푸시 잠금 구현으로 교체한다.
 */
@Slf4j
@Component
public class LogDeviceLockAdapter implements DeviceLockPort {

    @Override
    public void lock(UUID elderId) {
        log.info("[DEVICE-LOCK] elderId={} 기기 원격 잠금 요청", elderId);
    }
}
