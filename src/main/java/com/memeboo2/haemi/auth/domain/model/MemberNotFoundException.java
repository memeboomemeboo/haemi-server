package com.memeboo2.haemi.auth.domain.model;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String identifier) {
        super("회원을 찾을 수 없습니다: " + identifier);
    }
}
