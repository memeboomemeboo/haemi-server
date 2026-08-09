package com.memeboo2.haemi.offline.application;

import com.memeboo2.haemi.offline.domain.IngestOutcome;
import com.memeboo2.haemi.offline.domain.OfflineResultReceipt;
import com.memeboo2.haemi.offline.domain.OfflineSessionResult;
import com.memeboo2.haemi.offline.domain.port.OfflineResultApplyPort;
import com.memeboo2.haemi.offline.domain.repository.OfflineResultReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오프라인 세션 결과 멱등 수신 (#49). 같은 idempotencyKey 재전송은 1회만 적용하고,
 * 수신 영수증을 7일 보관 후 정리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfflineResultIngestService {

    private final OfflineResultReceiptRepository receiptRepository;
    private final OfflineResultApplyPort applyPort;

    @Value("${haemi.offline.retention-days:7}")
    private int retentionDays;

    @Transactional
    public IngestOutcome ingest(OfflineSessionResult result) {
        if (receiptRepository.existsByIdempotencyKey(result.idempotencyKey())) {
            log.debug("오프라인 결과 중복 수신, 멱등 무시: key={}", result.idempotencyKey());
            return IngestOutcome.DUPLICATE;
        }
        applyPort.apply(result);
        receiptRepository.save(OfflineResultReceipt.record(result, LocalDateTime.now()));
        log.info("오프라인 세션 결과 수신: key={}, elderId={}, sessionId={}",
                result.idempotencyKey(), result.elderId(), result.sessionId());
        return IngestOutcome.ACCEPTED;
    }

    @Transactional
    public BatchIngestResult ingestBatch(List<OfflineSessionResult> results) {
        int accepted = 0;
        int duplicate = 0;
        for (OfflineSessionResult result : results) {
            if (ingest(result) == IngestOutcome.ACCEPTED) {
                accepted++;
            } else {
                duplicate++;
            }
        }
        return new BatchIngestResult(accepted, duplicate);
    }

    // 7일 보관 정리
    @Transactional
    public int purgeExpired(LocalDateTime now) {
        int removed = receiptRepository.deleteReceivedBefore(now.minusDays(retentionDays));
        if (removed > 0) {
            log.info("오프라인 결과 영수증 정리: {}건 (보관 {}일 초과)", removed, retentionDays);
        }
        return removed;
    }
}
