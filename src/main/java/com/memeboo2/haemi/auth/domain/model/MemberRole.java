package com.memeboo2.haemi.auth.domain.model;

/**
 * 사용자 유형.
 * <ul>
 *   <li>FAMILY — 가족 (원격 참여자, B)</li>
 *   <li>ELDER — 치매 어르신 (A)</li>
 *   <li>INSTITUTION_ADMIN — 요양 기관 관리자 (C), 2FA 적용</li>
 * </ul>
 */
public enum MemberRole {
    FAMILY,
    ELDER,
    INSTITUTION_ADMIN
}
