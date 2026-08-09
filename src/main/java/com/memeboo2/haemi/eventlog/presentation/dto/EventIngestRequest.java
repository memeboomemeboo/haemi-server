package com.memeboo2.haemi.eventlog.presentation.dto;

import com.memeboo2.haemi.eventlog.domain.EventEnvelope;
import com.memeboo2.haemi.eventlog.domain.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "이벤트 로그 항목 (VAD 등은 durationMs만, 원문 없음)")
public record EventIngestRequest(
        @Schema(description = "멱등 키 (재전송 시 동일)")
        @NotBlank String idempotencyKey,
        @Schema(description = "어르신 ID (시스템 이벤트는 생략 가능)")
        String elderId,
        @NotNull EventType eventType,
        @NotNull LocalDateTime occurredAt,
        @Schema(description = "길이(ms) — VAD 등 길이 이벤트 전용")
        Integer durationMs,
        @Schema(description = "짧은 요약(원문 없음)")
        String detail
) {
    public EventEnvelope toEnvelope() {
        return new EventEnvelope(idempotencyKey, elderId, eventType, occurredAt, durationMs, detail);
    }
}
