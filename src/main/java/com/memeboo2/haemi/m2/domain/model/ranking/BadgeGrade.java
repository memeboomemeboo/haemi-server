package com.memeboo2.haemi.m2.domain.model.ranking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BadgeGrade {
    NONE("없음", 0),
    LOVE_SPROUT("사랑 새싹", 1),    // 별 1~4
    MEMORY_KING("추억왕", 5),       // 별 5~9
    MEMORY_KING_PLUS("추억킹", 10), // 별 10~19
    MEMORY_GOD("추억 갓", 20);      // 별 20+

    private final String label;
    private final int minStars;

    public static BadgeGrade fromStars(int stars) {
        if (stars >= MEMORY_GOD.minStars)      return MEMORY_GOD;
        if (stars >= MEMORY_KING_PLUS.minStars) return MEMORY_KING_PLUS;
        if (stars >= MEMORY_KING.minStars)      return MEMORY_KING;
        if (stars >= LOVE_SPROUT.minStars)      return LOVE_SPROUT;
        return NONE;
    }
}
