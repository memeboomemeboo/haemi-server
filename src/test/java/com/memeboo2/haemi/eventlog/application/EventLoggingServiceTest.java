package com.memeboo2.haemi.eventlog.application;

import com.memeboo2.haemi.eventlog.domain.EventCollectionConsent;
import com.memeboo2.haemi.eventlog.domain.EventEnvelope;
import com.memeboo2.haemi.eventlog.domain.EventIngestOutcome;
import com.memeboo2.haemi.eventlog.domain.EventType;
import com.memeboo2.haemi.eventlog.domain.EventTypeCount;
import com.memeboo2.haemi.eventlog.domain.LoggedEvent;
import com.memeboo2.haemi.eventlog.domain.repository.EventCollectionConsentRepository;
import com.memeboo2.haemi.eventlog.domain.repository.LoggedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventLoggingServiceTest {

    @Mock LoggedEventRepository events;
    @Mock EventCollectionConsentRepository consents;

    EventLoggingService service;

    @BeforeEach
    void setUp() {
        service = new EventLoggingService(events, consents);
    }

    private EventEnvelope envelope(String key, String elderId, EventType type) {
        return new EventEnvelope(key, elderId, type, LocalDateTime.now(), null, null);
    }

    @Test
    @DisplayName("동의한 어르신의 최초 이벤트는 수집되고 저장된다")
    void ingest_accepted() {
        when(consents.findByElderId("elder-1"))
                .thenReturn(Optional.of(EventCollectionConsent.granted("elder-1")));
        when(events.existsByIdempotencyKey("k1")).thenReturn(false);

        EventIngestOutcome outcome = service.ingest(envelope("k1", "elder-1", EventType.SESSION_START));

        assertThat(outcome).isEqualTo(EventIngestOutcome.ACCEPTED);
        verify(events).save(any(LoggedEvent.class));
    }

    @Test
    @DisplayName("F0-06: 동의 레코드가 없으면 수집을 거부한다")
    void ingest_rejectedWhenNoConsentRecord() {
        when(consents.findByElderId("elder-1")).thenReturn(Optional.empty());

        EventIngestOutcome outcome = service.ingest(envelope("k1", "elder-1", EventType.SESSION_START));

        assertThat(outcome).isEqualTo(EventIngestOutcome.REJECTED_NO_CONSENT);
        verify(events, never()).save(any());
    }

    @Test
    @DisplayName("어르신에 귀속되지 않은 시스템 이벤트는 동의와 무관하게 수집한다")
    void ingest_acceptsSystemEventWithoutElder() {
        when(events.existsByIdempotencyKey("k1")).thenReturn(false);

        EventIngestOutcome outcome = service.ingest(envelope("k1", null, EventType.SESSION_START));

        assertThat(outcome).isEqualTo(EventIngestOutcome.ACCEPTED);
        verify(events).save(any(LoggedEvent.class));
    }

    @Test
    @DisplayName("중복 재전송은 멱등하게 무시한다")
    void ingest_duplicate() {
        when(consents.findByElderId("elder-1"))
                .thenReturn(Optional.of(EventCollectionConsent.granted("elder-1")));
        when(events.existsByIdempotencyKey("k1")).thenReturn(true);

        EventIngestOutcome outcome = service.ingest(envelope("k1", "elder-1", EventType.SESSION_START));

        assertThat(outcome).isEqualTo(EventIngestOutcome.DUPLICATE);
        verify(events, never()).save(any());
    }

    @Test
    @DisplayName("동의 철회 상태면 수집을 거부한다")
    void ingest_rejectedWhenConsentWithdrawn() {
        EventCollectionConsent withdrawn = EventCollectionConsent.granted("elder-1");
        withdrawn.withdraw(LocalDateTime.now());
        when(consents.findByElderId("elder-1")).thenReturn(Optional.of(withdrawn));

        EventIngestOutcome outcome = service.ingest(envelope("k1", "elder-1", EventType.SESSION_START));

        assertThat(outcome).isEqualTo(EventIngestOutcome.REJECTED_NO_CONSENT);
        verify(events, never()).save(any());
    }

    @Test
    @DisplayName("배치는 수집/중복/거부를 집계한다")
    void ingestBatch_counts() {
        when(consents.findByElderId(any()))
                .thenReturn(Optional.of(EventCollectionConsent.granted("elder-1")));
        when(events.existsByIdempotencyKey("a")).thenReturn(false);
        when(events.existsByIdempotencyKey("b")).thenReturn(true);

        BatchEventResult batch = service.ingestBatch(List.of(
                envelope("a", "elder-1", EventType.HINT_SERVED),
                envelope("b", "elder-1", EventType.HINT_USED)));

        assertThat(batch.acceptedCount()).isEqualTo(1);
        assertThat(batch.duplicateCount()).isEqualTo(1);
        assertThat(batch.total()).isEqualTo(2);
    }

    @Test
    @DisplayName("동의 철회 시 수집을 중단하고 기존분을 가명 처리한다")
    void withdrawConsent_stopsAndPseudonymizes() {
        when(consents.findByElderId("elder-1")).thenReturn(Optional.empty());
        when(events.pseudonymizeByElderId("elder-1")).thenReturn(5);

        int pseudonymized = service.withdrawConsent("elder-1");

        assertThat(pseudonymized).isEqualTo(5);
        verify(consents).save(any(EventCollectionConsent.class));
        verify(events).pseudonymizeByElderId("elder-1");
    }

    @Test
    @DisplayName("일일 집계는 타입별 카운트와 총합을 낸다")
    void aggregateDaily_countsByType() {
        when(events.countByTypeBetween(any(), any())).thenReturn(List.of(
                new EventTypeCount(EventType.SESSION_START, 3),
                new EventTypeCount(EventType.VAD_DETECTED, 7)));

        DailyEventSummary summary = service.aggregateDaily(LocalDate.of(2026, 8, 5));

        assertThat(summary.total()).isEqualTo(10);
        assertThat(summary.countsByType()).containsEntry(EventType.SESSION_START, 3L)
                .containsEntry(EventType.VAD_DETECTED, 7L);
    }
}
