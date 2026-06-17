package com.memeboo2.haemi.auth.application.command;

import java.util.UUID;

public record VerifyTotpCommand(UUID memberId, String code) {}
