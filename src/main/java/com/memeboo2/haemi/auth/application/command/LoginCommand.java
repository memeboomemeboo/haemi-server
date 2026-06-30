package com.memeboo2.haemi.auth.application.command;

public record LoginCommand(
        String email,
        String password,
        /** 2FA TOTP 코드 (기관 관리자용, 선택) */
        String totpCode
) {}
