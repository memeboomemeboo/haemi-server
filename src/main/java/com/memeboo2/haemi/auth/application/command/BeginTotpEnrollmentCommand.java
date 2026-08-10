package com.memeboo2.haemi.auth.application.command;

/** 잠긴 기관 관리자의 2FA 최초 등록 시작 (#96). */
public record BeginTotpEnrollmentCommand(String email, String password) {}
