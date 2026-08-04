package com.memeboo2.haemi.m1.application.command;

import com.memeboo2.haemi.m1.domain.model.memory.MemoryMediaType;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public record CreateMemoryCommand(UUID groupId, UUID authorUserId, String textContent,
                                  MemoryVisibility visibility, List<MediaAttachment> media) {

    public record MediaAttachment(MemoryMediaType type, InputStream inputStream, String originalFilename,
                                  String contentType, long sizeBytes, Long durationMs) {
    }
}
