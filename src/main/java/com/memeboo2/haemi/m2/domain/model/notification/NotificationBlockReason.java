package com.memeboo2.haemi.m2.domain.model.notification;

/**
 * 어르신 알림 차단 사유.
 * 사별·입원 등 어르신 상태 기반 사유(EX-F201-05)는 어르신 상태 머신(#36)에 의존하므로
 * 본 정책에는 포함하지 않고 릴리스 게이트 #50에서 확장한다.
 */
public enum NotificationBlockReason {
    NONE,
    QUIET_HOURS,
    DAILY_LIMIT_EXCEEDED
}
