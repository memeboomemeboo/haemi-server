package com.memeboo2.haemi.offline.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 단말이 오프라인에서 완주한 세션의 결과 요약 (#49). idempotencyKey로 멱등 재전송을 식별한다.
 */
public record OfflineSessionResult(
        String idempotencyKey,
        String elderId,
        UUID sessionId,
        LocalDateTime completedAt,
        int respondedCount,
        int noResponseCount
) {}
