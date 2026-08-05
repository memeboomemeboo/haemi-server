package com.memeboo2.haemi.offline.infrastructure;

import com.memeboo2.haemi.offline.domain.OfflineResultReceipt;
import com.memeboo2.haemi.offline.domain.repository.OfflineResultReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class OfflineResultReceiptRepositoryAdapter implements OfflineResultReceiptRepository {

    private final JpaOfflineResultReceiptRepository jpa;

    @Override
    public OfflineResultReceipt save(OfflineResultReceipt receipt) {
        return jpa.save(receipt);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return jpa.existsById(idempotencyKey);
    }

    @Override
    public int deleteReceivedBefore(LocalDateTime cutoff) {
        return jpa.deleteByReceivedAtBefore(cutoff);
    }
}
