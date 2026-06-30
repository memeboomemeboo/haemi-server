package com.memeboo2.haemi.auth.domain.port;

import com.memeboo2.haemi.auth.domain.model.MemberRole;

import java.util.UUID;

/**
 * JWT 토큰 생성·검증 포트.
 */
public interface TokenPort {

    String generateAccessToken(UUID memberId, String email, MemberRole role);

    String generateRefreshToken(UUID memberId);

    UUID extractMemberId(String token);

    String extractEmail(String token);

    MemberRole extractRole(String token);

    boolean isValid(String token);

    /** Access Token 여부 (refresh token은 type=refresh 클레임 보유) */
    boolean isAccessToken(String token);

    /** Access Token을 블랙리스트에 추가하여 즉시 무효화 */
    void blacklistAccessToken(String token);

    /** Refresh Token을 SHA-256 해시하여 반환 */
    String hashToken(String token);
}
