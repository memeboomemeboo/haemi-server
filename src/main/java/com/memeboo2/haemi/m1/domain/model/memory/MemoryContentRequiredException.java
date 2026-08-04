package com.memeboo2.haemi.m1.domain.model.memory;

public class MemoryContentRequiredException extends RuntimeException {
    public MemoryContentRequiredException() {
        super("추억을 하나라도 남겨주세요.");
    }
}
