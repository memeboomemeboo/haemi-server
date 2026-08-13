package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/**
 * 어르신 기기 원격 잠금/복구 포트 (F0-05 사별 처리).
 * 실패는 예외로 표현하며 호출측이 아웃박스 재시도로 복구한다(EX-F005-06).
 */
public interface DeviceLockPort {

    void lock(UUID elderId);

    void unlock(UUID elderId);
}
