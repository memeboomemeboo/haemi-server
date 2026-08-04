package com.memeboo2.haemi.m1.application.service;

import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * F1-03 서버 측 최소 안전망. 클라이언트 검사는 우회될 수 있으므로 게시 직전에도 반드시 실행한다.
 * review 단어는 대표 보호자가 확인할 수 있게 별도 상태로만 보관하며 어르신 피드에는 노출하지 않는다.
 */
@Component
public class MemoryModerationService {

    private static final List<String> BLOCKED_WORDS = List.of(
            "죽어", "죽인다", "꺼져", "병신", "미친년", "미친놈", "혐오", "죽이고 싶");
    private static final List<String> REVIEW_WORDS = List.of("장례", "장례식", "사별", "중환자실");

    public MemoryModerationStatus inspect(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return MemoryModerationStatus.CLEAR;
        }
        String normalized = textContent.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (BLOCKED_WORDS.stream().anyMatch(normalized::contains)) {
            return MemoryModerationStatus.BLOCKED;
        }
        if (REVIEW_WORDS.stream().anyMatch(normalized::contains)) {
            return MemoryModerationStatus.REVIEW;
        }
        return MemoryModerationStatus.CLEAR;
    }
}
