package com.memeboo2.haemi.m0.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 사별 확정 이벤트 (F0-05). 다운스트림에서 스케줄 잡 취소·기기 원격 잠금·콘텐츠 억제를 수행한다.
 */
public record ElderBereavedEvent(
        UUID elderId,
        UUID groupId,
        LocalDateTime bereavedAt,
        LocalDateTime silentUntil
) {}
