package com.memeboo2.haemi.m1.domain.model.album;

public class MemoLengthExceededException extends RuntimeException {
    public MemoLengthExceededException(int length) {
        super("메모는 200자를 초과할 수 없습니다. 현재 길이=" + length);
    }
}
