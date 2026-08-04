package com.memeboo2.haemi.m0.domain.event;

import java.util.UUID;

/**
 * S1 안전 이벤트. M1 콘텐츠 큐·캐시·선다운로드본은 이 이벤트를 수신해 즉시 무효화해야 한다.
 */
public record PersonSafetyChangedEvent(UUID groupId, UUID personId) {
}
