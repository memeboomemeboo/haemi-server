package com.memeboo2.haemi.m5.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record VoiceAlarmAcknowledgedEvent(
        UUID alarmId,
        String elderId,
        String groupId,
        LocalDateTime acknowledgedAt
) {}
