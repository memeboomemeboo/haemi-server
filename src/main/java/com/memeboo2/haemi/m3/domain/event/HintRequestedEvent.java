package com.memeboo2.haemi.m3.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record HintRequestedEvent(
        UUID sessionId,
        String elderId,
        UUID albumId,
        String questionId,
        int usedCount,
        LocalDateTime requestedAt
) {}
