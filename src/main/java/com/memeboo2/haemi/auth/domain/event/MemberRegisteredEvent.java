package com.memeboo2.haemi.auth.domain.event;

import com.memeboo2.haemi.auth.domain.model.MemberRole;

import java.util.UUID;

public record MemberRegisteredEvent(
        UUID memberId,
        String email,
        String name,
        MemberRole role
) {}
