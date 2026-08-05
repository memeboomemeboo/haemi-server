package com.memeboo2.haemi.eventlog.application;

import com.memeboo2.haemi.eventlog.domain.EventCollectionConsent;
import com.memeboo2.haemi.eventlog.domain.EventEnvelope;
import com.memeboo2.haemi.eventlog.domain.EventIngestOutcome;
import com.memeboo2.haemi.eventlog.domain.EventType;
import com.memeboo2.haemi.eventlog.domain.EventTypeCount;
import com.memeboo2.haemi.eventlog.domain.LoggedEvent;
import com.memeboo2.haemi.eventlog.domain.repository.EventCollectionConsentRepository;
import com.memeboo2.haemi.eventlog.domain.repository.LoggedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 이벤트 로깅 파이프라인 (F0-06). 동의 확인 → 멱등 수집, 동의 철회 시 즉시 중단·가명 처리, 일일 집계.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLoggingService {

    private final LoggedEventRepository events;
    private final EventCollectionConsentRepository consents;

    @Transactional
    public EventIngestOutcome ingest(EventEnvelope envelope) {
        if (!isCollectible(envelope.elderId())) {
            return EventIngestOutcome.REJECTED_NO_CONSENT;
        }
        if (events.existsByIdempotencyKey(envelope.idempotencyKey())) {
            return EventIngestOutcome.DUPLICATE;
        }
        events.save(LoggedEvent.record(envelope, LocalDateTime.now()));
        return EventIngestOutcome.ACCEPTED;
    }

    @Transactional
    public BatchEventResult ingestBatch(List<EventEnvelope> envelopes) {
        int accepted = 0;
        int duplicate = 0;
        int rejected = 0;
        for (EventEnvelope envelope : envelopes) {
            switch (ingest(envelope)) {
                case ACCEPTED -> accepted++;
                case DUPLICATE -> duplicate++;
                case REJECTED_NO_CONSENT -> rejected++;
            }
        }
        return new BatchEventResult(accepted, duplicate, rejected);
    }

    // 동의 철회: 즉시 수집 중단 + 기존분 가명 처리
    @Transactional
    public int withdrawConsent(String elderId) {
        EventCollectionConsent consent = consents.findByElderId(elderId)
                .orElseGet(() -> EventCollectionConsent.granted(elderId));
        consent.withdraw(LocalDateTime.now());
        consents.save(consent);
        int pseudonymized = events.pseudonymizeByElderId(elderId);
        log.info("이벤트 수집 동의 철회: elderId={}, 가명처리={}건", elderId, pseudonymized);
        return pseudonymized;
    }

    // 06:00 일일 집계: 전일 타입별 카운트
    @Transactional(readOnly = true)
    public DailyEventSummary aggregateDaily(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.atTime(LocalTime.MAX);
        Map<EventType, Long> counts = new EnumMap<>(EventType.class);
        long total = 0;
        for (EventTypeCount row : events.countByTypeBetween(from, to)) {
            counts.put(row.eventType(), row.count());
            total += row.count();
        }
        log.info("일일 이벤트 집계: date={}, 총 {}건", date, total);
        return new DailyEventSummary(date, counts, total);
    }

    private boolean isCollectible(String elderId) {
        if (elderId == null || elderId.isBlank()) {
            return true; // 시스템 이벤트 등 어르신 비귀속 이벤트는 항상 수집
        }
        return consents.findByElderId(elderId)
                .map(EventCollectionConsent::isActive)
                .orElse(true); // 기록 없으면 기본 수집 허용
    }
}
