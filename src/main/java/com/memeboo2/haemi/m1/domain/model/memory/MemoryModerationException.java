package com.memeboo2.haemi.m1.domain.model.memory;

public class MemoryModerationException extends RuntimeException {
    public MemoryModerationException() {
        super("이 표현은 담을 수 없어요.");
    }
}
