package com.memeboo2.haemi.m2.domain.model.notification;

import com.memeboo2.haemi.common.exception.DomainValidationException;

import java.time.LocalTime;

/**
 * 어르신 추억 알림 발송 가부를 결정하는 순수 도메인 정책 (F2-01).
 * 일일 한도와 야간 차단 시간대를 검사한다.
 *
 * <p>사별·입원 등 어르신 상태 기반 차단(EX-F201-05)은 어르신 상태 머신(#36) 의존이라
 * 여기서 다루지 않는다. 추후 #50에서 상태 조회 결과를 이 정책 앞단에 결합한다.
 */
public class ElderNotificationPolicy {

    private final int dailyLimit;
    private final QuietHours quietHours;

    public ElderNotificationPolicy(int dailyLimit, QuietHours quietHours) {
        if (dailyLimit < 0) {
            throw new DomainValidationException("일일 알림 한도는 0 이상이어야 합니다: " + dailyLimit);
        }
        this.dailyLimit = dailyLimit;
        this.quietHours = quietHours;
    }

    /**
     * @param todaySentCount 오늘 이미 발송(집계)된 알림 수
     * @param now            발송 시도 시각
     */
    public NotificationDecision decide(int todaySentCount, LocalTime now) {
        // 한도 초과가 야간 여부보다 우선 — 하루 총량을 먼저 보장한다.
        if (todaySentCount >= dailyLimit) {
            return NotificationDecision.block(NotificationBlockReason.DAILY_LIMIT_EXCEEDED);
        }
        if (quietHours.covers(now)) {
            return NotificationDecision.block(NotificationBlockReason.QUIET_HOURS);
        }
        return NotificationDecision.allow();
    }
}
