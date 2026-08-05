package com.memeboo2.haemi.eventlog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 이벤트 수집 동의 상태 (F0-06). 철회 시 즉시 수집을 중단한다.
 */
@Entity
@Table(name = "event_collection_consent")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventCollectionConsent {

    @Id
    @Column(name = "elder_id", length = 255)
    private String elderId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public static EventCollectionConsent granted(String elderId) {
        EventCollectionConsent consent = new EventCollectionConsent();
        consent.elderId = elderId;
        consent.active = true;
        return consent;
    }

    public void withdraw(LocalDateTime now) {
        this.active = false;
        this.withdrawnAt = now;
    }
}
