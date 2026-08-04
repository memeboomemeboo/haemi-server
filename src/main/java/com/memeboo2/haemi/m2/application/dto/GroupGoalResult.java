package com.memeboo2.haemi.m2.application.dto;

import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 그룹 협력 목표 진행 상황. 개인 순위 없이 그룹 전체의 공동 진척만 노출한다.
 */
public record GroupGoalResult(
        UUID goalId,
        UUID albumId,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        int targetCount,
        int currentProgress,
        int remaining,
        int participantCount,
        boolean achieved,
        LocalDateTime achievedAt
) {
    public static GroupGoalResult from(GroupGoal goal) {
        return new GroupGoalResult(
                goal.getId(),
                goal.getAlbumId(),
                goal.getPeriod().name(),
                goal.getPeriodStart(),
                goal.getPeriodEnd(),
                goal.getTargetCount(),
                goal.getCurrentProgress(),
                goal.remaining(),
                goal.participantCount(),
                goal.isAchieved(),
                goal.getAchievedAt()
        );
    }
}
