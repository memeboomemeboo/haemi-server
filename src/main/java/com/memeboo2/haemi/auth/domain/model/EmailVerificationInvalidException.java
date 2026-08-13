package com.memeboo2.haemi.auth.domain.model;

public class EmailVerificationInvalidException extends RuntimeException {
    public EmailVerificationInvalidException() {
        super("이메일 확인 링크가 만료되었거나 이미 사용되었어요.");
    }
}
