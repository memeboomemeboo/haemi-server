package com.memeboo2.haemi.offline.presentation.dto;

import com.memeboo2.haemi.offline.domain.OfflineSessionResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "오프라인 완주 세션 결과 항목")
public record OfflineResultRequest(
        @Schema(description = "멱등 키 (재전송 시 동일)", example = "elder-1:2026-08-05:abc123")
        @NotBlank String idempotencyKey,
        @NotBlank String elderId,
        @NotNull UUID sessionId,
        @NotNull LocalDateTime completedAt,
        int respondedCount,
        int noResponseCount
) {
    public OfflineSessionResult toResult() {
        return new OfflineSessionResult(
                idempotencyKey, elderId, sessionId, completedAt, respondedCount, noResponseCount);
    }
}
