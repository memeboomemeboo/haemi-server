package com.memeboo2.haemi.predownload.infrastructure;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;
import com.memeboo2.haemi.predownload.domain.port.PredownloadDispatchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그 기반 선다운로드 전송 어댑터 (개발/테스트 전용).
 * 운영에서는 CDN 프리페치 / 디바이스 캐시 푸시 구현으로 교체한다.
 */
@Slf4j
@Component
public class LogPredownloadDispatchAdapter implements PredownloadDispatchPort {

    @Override
    public void dispatch(PredownloadBundle bundle) {
        log.info("[PREDOWNLOAD] elderId={} date={} cards={} photos={} hints={} (총 {}개)",
                bundle.elderId(), bundle.date(),
                bundle.cardKeys().size(), bundle.photoKeys().size(),
                bundle.hintKeys().size(), bundle.totalAssets());
    }
}
