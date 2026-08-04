package com.memeboo2.haemi.m2.domain.model.post;

public class InvalidHeartEmojiException extends RuntimeException {
    public InvalidHeartEmojiException(String code) {
        super("지원하지 않는 마음 이모지입니다: " + code);
    }
}
