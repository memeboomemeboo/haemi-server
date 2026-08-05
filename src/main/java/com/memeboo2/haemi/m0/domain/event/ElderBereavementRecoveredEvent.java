package com.memeboo2.haemi.m0.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사별 오등록 48시간 내 복구 이벤트 (F0-05). 취소된 잡·잠금을 원복하는 다운스트림 트리거.
 */
public record ElderBereavementRecoveredEvent(
        UUID elderId,
        UUID groupId,
        LocalDateTime recoveredAt
) {}
