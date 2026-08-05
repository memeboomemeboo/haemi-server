package com.memeboo2.haemi.m2.domain.model.notification;

import java.time.LocalTime;

/**
 * 어르신 야간 알림 차단 시간대. 시작~종료가 자정을 넘는 경우(예: 21시~08시)를 랩어라운드로 처리한다.
 * start와 end는 [0, 24) 범위의 시(hour).
 */
public record QuietHours(int startHour, int endHour) {

    public QuietHours {
        if (startHour < 0 || startHour > 23 || endHour < 0 || endHour > 23) {
            throw new IllegalArgumentException("야간 차단 시간대는 0~23시여야 합니다: " + startHour + "~" + endHour);
        }
    }

    /**
     * 주어진 시각이 야간 차단 구간에 속하는지. 경계는 [start, end) — 시작시 포함, 종료시 제외.
     */
    public boolean covers(LocalTime time) {
        int hour = time.getHour();
        if (startHour == endHour) {
            return false; // 빈 구간
        }
        if (startHour < endHour) {
            return hour >= startHour && hour < endHour;
        }
        // 자정 넘김: [start, 24) ∪ [0, end)
        return hour >= startHour || hour < endHour;
    }
}
