package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.memory.MemoryMediaType;

import java.util.UUID;

public record MemoryMediaResult(UUID mediaId, MemoryMediaType type, String accessUrl,
                                Long durationMs, int displayOrder) {
}
