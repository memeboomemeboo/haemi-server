package com.memeboo2.haemi.m2.domain.event;

import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 그룹 협력 목표 달성 이벤트. 개인 우승자가 아니라 가족 전체의 공동 성취로 발행된다.
 */
public record GroupGoalAchievedEvent(
        UUID goalId,
        UUID albumId,
        GoalPeriod period,
        int targetCount,
        int participantCount,
        LocalDateTime occurredAt
) {}
