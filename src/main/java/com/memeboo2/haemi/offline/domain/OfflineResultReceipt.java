package com.memeboo2.haemi.offline.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 오프라인 세션 결과 수신 영수증 (#49). idempotencyKey를 키로 완주 결과를 1회만 기록한다(멱등).
 * received_at 기준 7일 보관 후 정리된다.
 */
@Entity
@Table(name = "offline_result_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OfflineResultReceipt {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "responded_count", nullable = false)
    private int respondedCount;

    @Column(name = "no_response_count", nullable = false)
    private int noResponseCount;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public static OfflineResultReceipt record(OfflineSessionResult result, LocalDateTime receivedAt) {
        OfflineResultReceipt receipt = new OfflineResultReceipt();
        receipt.idempotencyKey = result.idempotencyKey();
        receipt.elderId = result.elderId();
        receipt.sessionId = result.sessionId();
        receipt.completedAt = result.completedAt();
        receipt.respondedCount = result.respondedCount();
        receipt.noResponseCount = result.noResponseCount();
        receipt.receivedAt = receivedAt;
        return receipt;
    }
}
