package com.memeboo2.haemi.offline.application;

import com.memeboo2.haemi.offline.domain.IngestOutcome;
import com.memeboo2.haemi.offline.domain.OfflineResultReceipt;
import com.memeboo2.haemi.offline.domain.OfflineSessionResult;
import com.memeboo2.haemi.offline.domain.port.OfflineResultApplyPort;
import com.memeboo2.haemi.offline.domain.repository.OfflineResultReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfflineResultIngestServiceTest {

    @Mock OfflineResultReceiptRepository receiptRepository;
    @Mock OfflineResultApplyPort applyPort;

    OfflineResultIngestService service;

    @BeforeEach
    void setUp() {
        service = new OfflineResultIngestService(receiptRepository, applyPort);
        ReflectionTestUtils.setField(service, "retentionDays", 7);
    }

    private OfflineSessionResult result(String key) {
        return new OfflineSessionResult(key, "elder-1", UUID.randomUUID(),
                LocalDateTime.of(2026, 8, 5, 9, 20), 4, 1);
    }

    @Test
    @DisplayName("최초 수신은 적용하고 영수증을 저장한다")
    void ingest_firstTimeAccepted() {
        when(receiptRepository.existsByIdempotencyKey("key-1")).thenReturn(false);

        IngestOutcome outcome = service.ingest(result("key-1"));

        assertThat(outcome).isEqualTo(IngestOutcome.ACCEPTED);
        verify(applyPort).apply(any(OfflineSessionResult.class));
        verify(receiptRepository).save(any(OfflineResultReceipt.class));
    }

    @Test
    @DisplayName("중복 재전송은 멱등하게 무시한다 (적용·저장 없음)")
    void ingest_duplicateIgnored() {
        when(receiptRepository.existsByIdempotencyKey("key-1")).thenReturn(true);

        IngestOutcome outcome = service.ingest(result("key-1"));

        assertThat(outcome).isEqualTo(IngestOutcome.DUPLICATE);
        verify(applyPort, never()).apply(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("배치는 신규/중복을 집계한다")
    void ingestBatch_countsAcceptedAndDuplicate() {
        when(receiptRepository.existsByIdempotencyKey("a")).thenReturn(false);
        when(receiptRepository.existsByIdempotencyKey("b")).thenReturn(true);
        when(receiptRepository.existsByIdempotencyKey("c")).thenReturn(false);

        BatchIngestResult batch = service.ingestBatch(List.of(result("a"), result("b"), result("c")));

        assertThat(batch.acceptedCount()).isEqualTo(2);
        assertThat(batch.duplicateCount()).isEqualTo(1);
        assertThat(batch.total()).isEqualTo(3);
        verify(applyPort, times(2)).apply(any());
    }

    @Test
    @DisplayName("보관 정리는 현재 시각에서 7일 이전을 컷오프로 삭제한다")
    void purgeExpired_usesSevenDayCutoff() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 4, 0);
        when(receiptRepository.deleteReceivedBefore(any())).thenReturn(3);

        int removed = service.purgeExpired(now);

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(receiptRepository).deleteReceivedBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(now.minusDays(7));
        assertThat(removed).isEqualTo(3);
    }
}
