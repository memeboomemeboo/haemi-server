package com.memeboo2.haemi.auth.application.dto;

import com.memeboo2.haemi.auth.domain.model.MemberRole;

import java.util.UUID;

public record TokenResult(
        UUID memberId,
        String email,
        String name,
        MemberRole role,
        String accessToken,
        String refreshToken,
        boolean totpEnabled
) {}
