package com.memeboo2.haemi.eventlog.domain;

import java.time.LocalDateTime;

/**
 * 단말이 전송하는 이벤트 봉투 (F0-06). idempotencyKey로 로컬 큐 멱등 재전송을 식별한다.
 * VAD 등 프라이버시 민감 이벤트는 durationMs만 담고 원문/음성은 담지 않는다.
 */
public record EventEnvelope(
        String idempotencyKey,
        String elderId,
        EventType eventType,
        LocalDateTime occurredAt,
        Integer durationMs,   // VAD 등 길이 이벤트에만 사용 (nullable)
        String detail         // 짧은 요약(원문 없음), nullable
) {}
