package com.memeboo2.haemi.m2.infrastructure.ai;

import com.memeboo2.haemi.m2.domain.port.AiPoemGeneratorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 실 서비스에서는 Claude API로 교체한다.
 */
@Slf4j
@Component
public class StubAiPoemGeneratorAdapter implements AiPoemGeneratorPort {

    @Override
    public String generatePoem(String postText) {
        log.info("[AI-STUB] 시 초안 생성: input='{}'", postText.substring(0, Math.min(20, postText.length())));
        return """
                봄날의 햇살처럼
                따뜻한 그 기억이
                내 마음 깊은 곳에
                오늘도 꽃으로 피어나네
                """.strip();
    }
}
