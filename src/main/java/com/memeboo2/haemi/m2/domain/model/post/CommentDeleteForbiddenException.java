package com.memeboo2.haemi.m2.domain.model.post;

import com.memeboo2.haemi.common.exception.DomainValidationException;

public class CommentDeleteForbiddenException extends DomainValidationException {
    public CommentDeleteForbiddenException() {
        super("본인이 작성한 댓글만 삭제할 수 있습니다.");
    }
}
