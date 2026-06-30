package com.memeboo2.haemi.auth.application.command;

import com.memeboo2.haemi.auth.domain.model.MemberRole;

public record SignUpCommand(
        String email,
        String password,
        String name,
        MemberRole role
) {}
