package com.memeboo2.haemi.m2.infrastructure.stt;

import com.memeboo2.haemi.m2.domain.port.SttPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 실 서비스에서는 Naver Clova Speech, Google STT 등으로 교체한다.
 */
@Slf4j
@Component
public class StubSttAdapter implements SttPort {

    @Override
    public String transcribe(InputStream audioStream, String contentType) {
        log.info("[STT-STUB] 음성 변환 요청: contentType={}", contentType);
        return "음성 메시지가 잘 도착했어요. 고맙구나.";
    }
}
