package com.memeboo2.haemi.m2.domain.event;

import com.memeboo2.haemi.m2.domain.model.ranking.BadgeGrade;

import java.time.LocalDateTime;
import java.util.UUID;

public record BadgeAwardedEvent(
        UUID albumId,
        String memberId,
        String memberName,
        BadgeGrade badgeGrade,
        LocalDateTime occurredAt
) {}
