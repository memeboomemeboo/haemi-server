package com.memeboo2.haemi.m2.domain.model.post;

import com.memeboo2.haemi.common.exception.DomainValidationException;

public class CommentContentRequiredException extends DomainValidationException {
    public CommentContentRequiredException() {
        super("댓글 내용을 입력해주세요.");
    }
}
