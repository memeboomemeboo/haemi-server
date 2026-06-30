package com.memeboo2.haemi.m1.domain.model.album;

public class PersonTagLimitExceededException extends RuntimeException {
    public PersonTagLimitExceededException(int count) {
        super("최대 10명까지 태그 가능합니다. 시도=" + count);
    }
}
