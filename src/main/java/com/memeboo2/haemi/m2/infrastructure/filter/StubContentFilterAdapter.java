package com.memeboo2.haemi.m2.infrastructure.filter;

import com.memeboo2.haemi.m2.domain.port.ContentFilterPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 실 서비스에서는 외부 욕설 감지 API(예: Naver Clova, kakao karlo 등)로 교체한다.
 */
@Slf4j
@Component
public class StubContentFilterAdapter implements ContentFilterPort {

    private static final Set<String> FORBIDDEN_WORDS = Set.of(
            "욕설1", "욕설2", "혐오표현1"
    );

    @Override
    public boolean containsForbiddenWord(String text) {
        if (text == null || text.isBlank()) return false;
        boolean found = FORBIDDEN_WORDS.stream().anyMatch(text::contains);
        if (found) log.warn("[FILTER-STUB] 금칙어 감지");
        return found;
    }
}
