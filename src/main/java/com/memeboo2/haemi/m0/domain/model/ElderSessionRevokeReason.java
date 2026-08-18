package com.memeboo2.haemi.m0.domain.model;

/** 어르신 평생 세션을 끊은 이유 (F0-01-E ⑥, EX-F005-06). */
public enum ElderSessionRevokeReason {
    /** 같은 기기에서 다시 합류해 세션을 재발급했다. */
    REISSUED,
    /** 사별·추모 등 어르신 상태 변경으로 기기를 잠갔다. */
    ELDER_STATUS_CHANGED,
    /** owner가 어르신 기기 연결을 초기화했다. */
    OWNER_REVOKED
}
