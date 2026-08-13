package com.memeboo2.haemi.auth.domain.model;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("이메일 확인을 완료해주세요.");
    }
}
