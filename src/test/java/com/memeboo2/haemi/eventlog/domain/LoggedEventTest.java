package com.memeboo2.haemi.eventlog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LoggedEventTest {

    @Test
    @DisplayName("VAD 이벤트는 발생·길이만 담고 원문은 담지 않는다")
    void vad_storesDurationNoContent() {
        EventEnvelope vad = new EventEnvelope("k1", "elder-1", EventType.VAD_DETECTED,
                LocalDateTime.of(2026, 8, 5, 9, 10), 1200, null);

        LoggedEvent event = LoggedEvent.record(vad, LocalDateTime.now());

        assertThat(event.getEventType()).isEqualTo(EventType.VAD_DETECTED);
        assertThat(event.getDurationMs()).isEqualTo(1200);
        assertThat(event.getDetail()).isNull();
        assertThat(event.isPseudonymized()).isFalse();
    }

    @Test
    @DisplayName("가명 처리는 어르신 식별자를 제거한다")
    void pseudonymize_removesElderId() {
        LoggedEvent event = LoggedEvent.record(
                new EventEnvelope("k2", "elder-1", EventType.SESSION_START,
                        LocalDateTime.now(), null, null),
                LocalDateTime.now());

        event.pseudonymize();

        assertThat(event.getElderId()).isNull();
        assertThat(event.isPseudonymized()).isTrue();
    }

    @Test
    @DisplayName("이벤트 유형은 11종이다")
    void eventTypes_areEleven() {
        assertThat(EventType.values()).hasSize(11);
    }
}
