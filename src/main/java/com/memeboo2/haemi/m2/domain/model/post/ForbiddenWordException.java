package com.memeboo2.haemi.m2.domain.model.post;

public class ForbiddenWordException extends RuntimeException {
    public ForbiddenWordException() {
        super("부적절한 표현이 포함되어 있습니다.");
    }
}
