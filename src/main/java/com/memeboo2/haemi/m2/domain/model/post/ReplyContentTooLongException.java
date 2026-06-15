package com.memeboo2.haemi.m2.domain.model.post;

public class ReplyContentTooLongException extends RuntimeException {
    public ReplyContentTooLongException(String message) {
        super(message);
    }
}
