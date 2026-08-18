package com.memeboo2.haemi.m2.domain.model.post;

import com.memeboo2.haemi.common.exception.DomainValidationException;

public class CommentContentTooLongException extends DomainValidationException {
    public CommentContentTooLongException(int length) {
        super("댓글은 최대 200자까지 입력 가능합니다. 현재 " + length + "자");
    }
}
