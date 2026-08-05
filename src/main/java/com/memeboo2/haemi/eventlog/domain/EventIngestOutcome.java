package com.memeboo2.haemi.eventlog.domain;

public enum EventIngestOutcome {
    ACCEPTED,             // 최초 수집·저장
    DUPLICATE,            // 이미 수집됨 (멱등 무시)
    REJECTED_NO_CONSENT   // 동의 철회 상태 — 수집 중단
}
