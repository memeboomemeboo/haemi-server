package com.memeboo2.haemi.m5.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record WalkCompletedEvent(
        UUID walkRecordId,
        String elderId,
        String groupId,
        int durationMinutes,
        int stepCount,
        LocalDateTime completedAt
) {}
