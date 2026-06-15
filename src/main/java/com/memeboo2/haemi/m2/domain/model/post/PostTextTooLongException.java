package com.memeboo2.haemi.m2.domain.model.post;

public class PostTextTooLongException extends RuntimeException {
    public PostTextTooLongException(int length) {
        super("본문은 최대 500자까지 입력 가능합니다. 현재 " + length + "자");
    }
}
