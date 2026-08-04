package com.memeboo2.haemi.m0.domain.model.access;

/**
 * 접근 모드 게이팅 대상 기능 (F0-03). Mode B(보조)는 안전 하위집합만 허용한다.
 */
public enum ModeFeature {
    SELF_SESSION_START,   // 어르신 직접 세션 시작
    DIRECT_REPLY,         // 추억글 직접 답변
    FEED_BROWSE,          // 피드 자유 탐색
    ALARM_SELF_MANAGE,    // 알람 직접 관리
    RECEIVE_NOTIFICATION  // 알림 수신 (양 모드 공통)
}
