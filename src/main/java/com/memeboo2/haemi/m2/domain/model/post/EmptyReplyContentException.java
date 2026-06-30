package com.memeboo2.haemi.m2.domain.model.post;

public class EmptyReplyContentException extends RuntimeException {
    public EmptyReplyContentException() {
        super("답변 내용을 입력해주세요.");
    }
}
