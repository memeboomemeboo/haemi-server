package com.memeboo2.haemi.offline.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineResultReceiptTest {

    @Test
    @DisplayName("결과로부터 수신 영수증을 생성한다")
    void record_fromResult() {
        UUID sessionId = UUID.randomUUID();
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 5, 9, 20);
        LocalDateTime receivedAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        OfflineSessionResult result = new OfflineSessionResult(
                "key-1", "elder-1", sessionId, completedAt, 4, 1);

        OfflineResultReceipt receipt = OfflineResultReceipt.record(result, receivedAt);

        assertThat(receipt.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(receipt.getElderId()).isEqualTo("elder-1");
        assertThat(receipt.getSessionId()).isEqualTo(sessionId);
        assertThat(receipt.getCompletedAt()).isEqualTo(completedAt);
        assertThat(receipt.getRespondedCount()).isEqualTo(4);
        assertThat(receipt.getNoResponseCount()).isEqualTo(1);
        assertThat(receipt.getReceivedAt()).isEqualTo(receivedAt);
    }
}
