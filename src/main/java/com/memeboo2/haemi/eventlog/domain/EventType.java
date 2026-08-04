package com.memeboo2.haemi.eventlog.domain;

/**
 * 수집 이벤트 11종 (F0-06). session_start ~ system_error.
 */
public enum EventType {
    SESSION_START,
    SESSION_COMPLETE,
    SESSION_ABANDON,
    HINT_SERVED,
    HINT_USED,
    NOTIFICATION_SENT,
    NOTIFICATION_ACK,
    ALARM_TRIGGERED,
    VAD_DETECTED,
    CONSENT_CHANGED,
    SYSTEM_ERROR
}
