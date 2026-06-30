package com.memeboo2.haemi.auth.application.command;

import java.util.UUID;

public record ChangePasswordCommand(
        UUID memberId,
        String currentPassword,
        String newPassword
) {}
