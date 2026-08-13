package com.memeboo2.haemi.predownload.infrastructure;

import com.memeboo2.haemi.predownload.domain.PredownloadBundle;
import com.memeboo2.haemi.predownload.domain.port.PredownloadDispatchPort;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그 기반 선다운로드 전송 어댑터 (개발/테스트 전용).
 * 운영에서는 CDN 프리페치 / 디바이스 캐시 푸시 구현으로 교체한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogPredownloadDispatchAdapter implements PredownloadDispatchPort {

    private final PushDispatchService pushDispatch;

    @Override
    public void dispatch(PredownloadBundle bundle) {
        log.info("[PREDOWNLOAD] elderId={} date={} cards={} photos={} hints={} (총 {}개)",
                bundle.elderId(), bundle.date(),
                bundle.cardKeys().size(), bundle.photoKeys().size(),
                bundle.hintKeys().size(), bundle.totalAssets());
        pushDispatch.dispatchToElder(bundle.elderId(), new PushMessage(
                "오늘의 회상을 준비했어요", "인터넷 없이도 회상을 이어갈 수 있어요.",
                java.util.Map.of(
                        "type", "PREDOWNLOAD",
                        "date", bundle.date().toString(),
                        "cardKeys", String.join(",", bundle.cardKeys()),
                        "photoKeys", String.join(",", bundle.photoKeys()),
                        "hintKeys", String.join(",", bundle.hintKeys())
                )));
    }
}
