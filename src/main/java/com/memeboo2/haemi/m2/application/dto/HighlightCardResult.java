package com.memeboo2.haemi.m2.application.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 기간 하이라이트 카드. 개인 랭킹이 아니라 그룹 전체의 성취와
 * "가족이 가장 사랑한 추억" 한 건을 축하 카드로 보여준다.
 */
public record HighlightCardResult(
        UUID albumId,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        boolean goalAchieved,
        int targetCount,
        int currentProgress,
        int participantCount,
        int totalPosts,
        int elderReplyCount,
        int totalLikes,
        TopMemory topMemory
) {
    public record TopMemory(
            UUID postId,
            String authorName,
            String relation,
            int likeCount,
            String preview
    ) {}
}
