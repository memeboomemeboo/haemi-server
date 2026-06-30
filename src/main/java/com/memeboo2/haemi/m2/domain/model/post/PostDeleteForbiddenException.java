package com.memeboo2.haemi.m2.domain.model.post;

public class PostDeleteForbiddenException extends RuntimeException {
    public PostDeleteForbiddenException() {
        super("본인이 작성한 추억글만 삭제할 수 있습니다.");
    }
}
