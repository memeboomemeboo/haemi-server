package com.memeboo2.haemi.m0.domain.model;

/**
 * 초대 경로 구분 (F0-01, F0-01-E).
 *
 * <p>가족(member)은 카카오톡·SMS 링크 토큰으로, 어르신(elder)은 어르신 화면에서 입력하는
 * 6자리 숫자 코드로 합류한다. 어르신은 member 정원(10명)과 무관한 그룹당 1인 프로필이다.
 */
public enum InvitationKind {
    FAMILY,
    ELDER
}
