package com.memeboo2.haemi.auth.application.dto;

import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.model.MemberStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberResult(
        UUID id,
        String email,
        String name,
        MemberRole role,
        MemberStatus status,
        UUID groupId,
        boolean totpEnabled,
        LocalDateTime createdAt
) {
    public static MemberResult from(Member member) {
        return from(member, null);
    }

    public static MemberResult from(Member member, UUID groupId) {
        return new MemberResult(
                member.getId(), member.getEmail(), member.getName(),
                member.getRole(), member.getStatus(), groupId,
                member.isTotpEnabled(), member.getCreatedAt()
        );
    }
}
