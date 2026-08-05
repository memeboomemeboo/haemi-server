package com.memeboo2.haemi.m0.domain.port;

import java.util.UUID;

/**
 * 어르신 가족 그룹 멤버십 조회 계약 (#51 도메인 계약).
 * 다운스트림(M3 등)이 "요청자가 해당 어르신의 가족 그룹 구성원인가"를 검증하는 seam.
 */
public interface ElderMembershipQuery {

    /**
     * {@code memberId}가 {@code elderId} 어르신의 활성 가족 그룹 구성원이면 true.
     * 존재하지 않는 어르신·그룹이거나 elderId가 유효한 UUID가 아니면 안전하게 false.
     */
    boolean isActiveGroupMember(String elderId, UUID memberId);
}
