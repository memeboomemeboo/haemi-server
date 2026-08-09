package com.memeboo2.haemi.m2.application.command;

import java.util.UUID;

/**
 * 그룹 협력 목표 진척 기록. amount 만큼 공동 진척을 올리고 contributorId를 참여자로 추가한다.
 */
public record RecordGoalContributionCommand(
        UUID albumId,
        int amount,
        String contributorId
) {}
