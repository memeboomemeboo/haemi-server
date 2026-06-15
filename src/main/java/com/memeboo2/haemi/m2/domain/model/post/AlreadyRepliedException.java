package com.memeboo2.haemi.m2.domain.model.post;

public class AlreadyRepliedException extends RuntimeException {
    public AlreadyRepliedException() {
        super("이미 답변한 추억글입니다.");
    }
}
