package com.memeboo2.haemi.m1.application.service;

import java.util.UUID;

public class MemoryNotFoundException extends RuntimeException {
    public MemoryNotFoundException(UUID memoryId) {
        super("추억을 찾을 수 없어요: " + memoryId);
    }
}
