package com.memeboo2.haemi.auth.domain.model;

import java.util.UUID;

public class AlreadyWithdrawnException extends RuntimeException {
    public AlreadyWithdrawnException(UUID memberId) {
        super("이미 탈퇴한 회원입니다: " + memberId);
    }
}
