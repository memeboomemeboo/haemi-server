package com.memeboo2.haemi.m2.domain.model.post;

public class MemoryPostNotFoundException extends RuntimeException {
    public MemoryPostNotFoundException(String id) {
        super("추억글을 찾을 수 없습니다. id=" + id);
    }
}
