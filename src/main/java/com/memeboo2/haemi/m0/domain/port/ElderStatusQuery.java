package com.memeboo2.haemi.m0.domain.port;

import com.memeboo2.haemi.m0.domain.model.ElderStatus;

import java.util.UUID;

/**
 * 어르신 상태 조회 계약 (#51 도메인 계약). 발송 파이프라인 등 다운스트림 seam이 소비한다.
 * 사별/입원/무음기간 어르신은 {@link #isDispatchable(UUID)}가 false를 반환한다.
 */
public interface ElderStatusQuery {

    ElderStatus statusOf(UUID elderId);

    boolean isDispatchable(UUID elderId);
}
