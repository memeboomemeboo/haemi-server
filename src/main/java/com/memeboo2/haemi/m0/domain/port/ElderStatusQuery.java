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

    /**
     * 어르신 ID를 문자열(UUID 표현)로 다루는 다운스트림 서비스용 오버로드.
     * 유효한 UUID가 아니거나 존재하지 않는 어르신은 안전하게 발송 불가로 처리한다.
     */
    boolean isDispatchable(String elderId);

    /**
     * 그룹 단위 발송 가능 여부. 해당 그룹의 어르신이 사별/입원/무음기간이면 false.
     * <p>그룹에 매핑된 어르신을 찾지 못하면 {@code true}(fail-open) — 그룹↔어르신 매핑이
     * 아직 확립되지 않은 호출자(M2 등)의 기존 동작을 보존하기 위함이다. 매핑이 확립되면
     * 별도 변경 없이 사별/입원 어르신에 대한 차단이 활성화된다.
     */
    boolean isGroupDispatchable(UUID groupId);
}
