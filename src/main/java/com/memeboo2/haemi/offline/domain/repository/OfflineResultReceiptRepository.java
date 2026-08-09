package com.memeboo2.haemi.offline.domain.repository;

import com.memeboo2.haemi.offline.domain.OfflineResultReceipt;

import java.time.LocalDateTime;

public interface OfflineResultReceiptRepository {

    OfflineResultReceipt save(OfflineResultReceipt receipt);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // 7일 보관 정리: 기준 시각 이전 수신 영수증 삭제, 삭제 건수 반환
    int deleteReceivedBefore(LocalDateTime cutoff);
}
