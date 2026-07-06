package com.memeboo2.haemi.m3.domain.event;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record HintRequestedEvent(
        UUID sessionId,
        String elderId,
        UUID albumId,
        String questionId,
        int usedCount,
        Set<String> recipientMemberIds,
        LocalDateTime requestedAt
) {
    public HintRequestedEvent {
        recipientMemberIds = recipientMemberIds == null ? Set.of() : Set.copyOf(recipientMemberIds);
    }
}
