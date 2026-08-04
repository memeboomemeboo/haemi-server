package com.memeboo2.haemi.m2.domain.model.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuietHoursTest {

    @Test
    @DisplayName("자정을 넘는 구간(21~08)은 밤·새벽을 포함하고 낮은 제외한다")
    void wrapAroundWindow() {
        QuietHours quiet = new QuietHours(21, 8);

        assertThat(quiet.covers(LocalTime.of(23, 0))).isTrue();  // 밤
        assertThat(quiet.covers(LocalTime.of(0, 0))).isTrue();   // 자정
        assertThat(quiet.covers(LocalTime.of(5, 30))).isTrue();  // 새벽
        assertThat(quiet.covers(LocalTime.of(8, 0))).isFalse();  // 종료 경계 제외
        assertThat(quiet.covers(LocalTime.of(12, 0))).isFalse(); // 낮
        assertThat(quiet.covers(LocalTime.of(20, 59))).isFalse();// 시작 직전
    }

    @Test
    @DisplayName("같은 날 안의 구간(09~17)은 랩어라운드 없이 판정한다")
    void sameDayWindow() {
        QuietHours quiet = new QuietHours(9, 17);

        assertThat(quiet.covers(LocalTime.of(8, 59))).isFalse();
        assertThat(quiet.covers(LocalTime.of(9, 0))).isTrue();
        assertThat(quiet.covers(LocalTime.of(16, 59))).isTrue();
        assertThat(quiet.covers(LocalTime.of(17, 0))).isFalse();
    }

    @Test
    @DisplayName("시작과 종료가 같으면 빈 구간이다")
    void emptyWindow() {
        assertThat(new QuietHours(0, 0).covers(LocalTime.of(3, 0))).isFalse();
    }

    @Test
    @DisplayName("범위를 벗어난 시(hour)는 거부한다")
    void rejectsInvalidHours() {
        assertThatThrownBy(() -> new QuietHours(24, 8))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
