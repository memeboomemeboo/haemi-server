package com.memeboo2.haemi.m2.domain.model.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ElderNotificationPolicyTest {

    private final ElderNotificationPolicy policy =
            new ElderNotificationPolicy(3, new QuietHours(21, 8));

    @Test
    @DisplayName("한도 미만이고 주간이면 발송을 허용한다")
    void allowsWithinLimitDuringDay() {
        NotificationDecision decision = policy.decide(2, LocalTime.of(15, 0));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.reason()).isEqualTo(NotificationBlockReason.NONE);
    }

    @Test
    @DisplayName("일일 한도에 도달하면 주간이라도 차단한다")
    void blocksWhenDailyLimitReached() {
        NotificationDecision decision = policy.decide(3, LocalTime.of(15, 0));

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.reason()).isEqualTo(NotificationBlockReason.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("야간 시간대에는 한도 미만이어도 차단한다")
    void blocksDuringQuietHours() {
        NotificationDecision decision = policy.decide(0, LocalTime.of(22, 30));

        assertThat(decision.blocked()).isTrue();
        assertThat(decision.reason()).isEqualTo(NotificationBlockReason.QUIET_HOURS);
    }

    @Test
    @DisplayName("한도 초과가 야간 차단보다 우선한다")
    void limitTakesPrecedenceOverQuietHours() {
        NotificationDecision decision = policy.decide(5, LocalTime.of(23, 0));

        assertThat(decision.reason()).isEqualTo(NotificationBlockReason.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("야간 경계: 20:59 허용, 21:00·07:59 차단, 08:00 허용")
    void quietHoursBoundaries() {
        assertThat(policy.decide(0, LocalTime.of(20, 59)).allowed()).isTrue();
        assertThat(policy.decide(0, LocalTime.of(21, 0)).blocked()).isTrue();
        assertThat(policy.decide(0, LocalTime.of(7, 59)).blocked()).isTrue();
        assertThat(policy.decide(0, LocalTime.of(8, 0)).allowed()).isTrue();
    }
}
