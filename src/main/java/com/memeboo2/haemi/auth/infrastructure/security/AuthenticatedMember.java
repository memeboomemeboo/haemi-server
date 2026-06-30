package com.memeboo2.haemi.auth.infrastructure.security;

import com.memeboo2.haemi.auth.domain.model.MemberRole;

import java.util.UUID;

/**
 * SecurityContext에 보관되는 인증된 사용자 정보.
 */
public record AuthenticatedMember(
        UUID memberId,
        String email,
        MemberRole role
) {}
