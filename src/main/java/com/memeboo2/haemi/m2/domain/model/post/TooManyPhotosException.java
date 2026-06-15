package com.memeboo2.haemi.m2.domain.model.post;

public class TooManyPhotosException extends RuntimeException {
    public TooManyPhotosException(int count) {
        super("사진은 최대 3장까지 첨부 가능합니다. 시도=" + count);
    }
}
