package com.memeboo2.haemi.m2.application.service;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(String commentId) {
        super("댓글을 찾을 수 없어요: " + commentId);
    }
}
