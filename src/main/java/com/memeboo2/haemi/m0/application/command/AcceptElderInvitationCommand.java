package com.memeboo2.haemi.m0.application.command;

/** F0-01-E 어르신 참여 로그인 입력. 전화번호는 해시로만 보관하며 OTP는 사용하지 않는다. */
public record AcceptElderInvitationCommand(String code, String name, String phoneNumber, String deviceId) {
}
