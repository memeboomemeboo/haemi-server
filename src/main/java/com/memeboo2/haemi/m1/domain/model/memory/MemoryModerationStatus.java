package com.memeboo2.haemi.m1.domain.model.memory;

/** 서버 모더레이션 결과. BLOCKED 상태의 추억은 저장하지 않는다. */
public enum MemoryModerationStatus {
    CLEAR,
    REVIEW,
    BLOCKED
}
