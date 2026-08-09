package com.memeboo2.haemi.m4.application.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** F4-02 발송 직전의 S1 어휘 게이트. 판정·치료·점수 표현은 어떤 경로에서도 발송하지 않는다. */
@Component
public class ActivityChangeLanguagePolicy {

    private static final List<String> FORBIDDEN = List.of(
            "인지 기능", "치매가 진행", "정답률", "정답", "오답", "인지 점수", "점수", "개선", "악화",
            "치료", "회복", "예측", "진단", "반응 시간");

    public void requireSafe(String message) {
        String normalized = message == null ? "" : message.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        if (FORBIDDEN.stream().anyMatch(normalized::contains)) {
            throw new IllegalArgumentException("활동 안내에 사용할 수 없는 판정 표현이 포함되어 있어요.");
        }
    }
}
