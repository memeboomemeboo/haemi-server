package com.memeboo2.haemi.m0.domain.model;

import java.util.Set;

/**
 * 어르신 상태 (F0-05). 전이 규칙은 {@link #canTransitionTo(ElderStatus)}로 강제한다.
 * DECEASED는 무음기간 경과 후 MEMORIAL로, 48시간 내 오등록이면 ACTIVE로만 전이 가능.
 * MEMORIAL은 종결 상태(열람·다운로드 전용).
 */
public enum ElderStatus {
    ACTIVE,
    DECLINING,
    HOSPITALIZED,
    DORMANT,
    DECEASED,
    MEMORIAL;

    private static final Set<ElderStatus> LIVING = Set.of(ACTIVE, DECLINING, HOSPITALIZED, DORMANT);

    public boolean canTransitionTo(ElderStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            // 생존 상태끼리는 자유 전이하며, 사별(DECEASED)로 진입 가능
            case ACTIVE, DECLINING, HOSPITALIZED, DORMANT ->
                    LIVING.contains(target) || target == DECEASED;
            // 사별: 무음기간 경과 시 MEMORIAL, 48h 오등록이면 ACTIVE 복구
            case DECEASED -> target == MEMORIAL || target == ACTIVE;
            // 종결
            case MEMORIAL -> false;
        };
    }

    public boolean isLiving() {
        return LIVING.contains(this);
    }
}
