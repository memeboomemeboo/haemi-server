package com.memeboo2.haemi.m1.domain.model.memory;

public class MemoryAccessDeniedException extends RuntimeException {
    public MemoryAccessDeniedException() {
        super("대표 보호자 또는 작성자만 추억을 삭제할 수 있어요.");
    }
}
