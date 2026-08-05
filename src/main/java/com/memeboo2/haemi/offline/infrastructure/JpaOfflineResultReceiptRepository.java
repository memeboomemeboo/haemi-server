package com.memeboo2.haemi.offline.infrastructure;

import com.memeboo2.haemi.offline.domain.OfflineResultReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface JpaOfflineResultReceiptRepository extends JpaRepository<OfflineResultReceipt, String> {

    @Modifying
    @Query("DELETE FROM OfflineResultReceipt r WHERE r.receivedAt < :cutoff")
    int deleteByReceivedAtBefore(LocalDateTime cutoff);
}
