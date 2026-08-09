package com.memeboo2.haemi.eventlog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 적재된 이벤트 (F0-06). idempotencyKey를 키로 멱등 저장한다.
 * VAD 등은 durationMs만 담고 원문/음성은 저장하지 않는다. 동의 철회 시 elderId를 제거해 가명 처리한다.
 */
@Entity
@Table(name = "logged_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoggedEvent {

    @Id
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "elder_id")
    private String elderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "detail", length = 200)
    private String detail;

    @Column(name = "pseudonymized", nullable = false)
    private boolean pseudonymized;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public static LoggedEvent record(EventEnvelope envelope, LocalDateTime receivedAt) {
        LoggedEvent event = new LoggedEvent();
        event.idempotencyKey = envelope.idempotencyKey();
        event.elderId = envelope.elderId();
        event.eventType = envelope.eventType();
        event.occurredAt = envelope.occurredAt();
        event.durationMs = envelope.durationMs();
        event.detail = envelope.detail();
        event.pseudonymized = false;
        event.receivedAt = receivedAt;
        return event;
    }

    // 동의 철회 시 기존분 가명 처리: 어르신 식별자를 제거한다.
    public void pseudonymize() {
        this.elderId = null;
        this.pseudonymized = true;
    }
}
