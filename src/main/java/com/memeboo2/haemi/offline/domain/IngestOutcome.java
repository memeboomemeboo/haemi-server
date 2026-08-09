package com.memeboo2.haemi.offline.domain;

public enum IngestOutcome {
    ACCEPTED,   // 최초 수신 — 적용·기록됨
    DUPLICATE   // 이미 수신된 결과 — 멱등 무동작
}
