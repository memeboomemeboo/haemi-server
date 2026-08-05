package com.memeboo2.haemi.m0.domain.model.access;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;

import java.util.EnumSet;
import java.util.Set;

/**
 * 모드별 기능 게이팅 (F0-03). Mode A(자립)는 전체 허용, Mode B(보조)는 안전 하위집합만 허용한다.
 */
public final class AccessModeGate {

    // Mode B(보조)에서 허용되는 기능 — 나머지는 보호자 대행이 필요
    private static final Set<ModeFeature> MODE_B_ALLOWED =
            EnumSet.of(ModeFeature.RECEIVE_NOTIFICATION, ModeFeature.FEED_BROWSE);

    private AccessModeGate() {
    }

    public static boolean isEnabled(ElderAccessMode mode, ModeFeature feature) {
        if (mode == null || feature == null) {
            return false;
        }
        return switch (mode) {
            case A -> true;                       // 자립: 전체 허용
            case B -> MODE_B_ALLOWED.contains(feature); // 보조: 하위집합
            case UNSET -> false;                  // 미설정: 판정 전 차단
        };
    }
}
