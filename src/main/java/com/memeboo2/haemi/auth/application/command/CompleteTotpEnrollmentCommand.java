package com.memeboo2.haemi.auth.application.command;

/** 잠긴 기관 관리자의 2FA 최초 등록 확정 (#96). */
public record CompleteTotpEnrollmentCommand(String email, String password, String secret, String code) {}
