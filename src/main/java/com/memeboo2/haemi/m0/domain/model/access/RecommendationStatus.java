package com.memeboo2.haemi.m0.domain.model.access;

public enum RecommendationStatus {
    PROPOSED,   // 제안됨 (임의 변경 금지 — 적용 대기)
    APPLIED,    // 가족이 명시적으로 적용
    DISMISSED   // 기각
}
