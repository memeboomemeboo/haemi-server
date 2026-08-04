package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/**
 * 어르신 기기 원격 잠금 포트 (F0-05 사별 처리). 실패는 예외로 표현하며 호출측이 복구를 처리한다(EX-F005-06).
 */
public interface DeviceLockPort {

    void lock(UUID elderId);
}
