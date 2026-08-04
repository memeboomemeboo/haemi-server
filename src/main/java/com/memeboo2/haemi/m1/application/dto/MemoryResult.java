package com.memeboo2.haemi.m1.application.dto;

import com.memeboo2.haemi.m1.domain.model.memory.Memory;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;
import com.memeboo2.haemi.m1.domain.port.PhotoStoragePort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MemoryResult(UUID memoryId, UUID groupId, UUID authorUserId, String authorName,
                           String authorRelation, String textContent, MemoryVisibility visibility,
                           MemoryModerationStatus moderationStatus, boolean pinned,
                           LocalDateTime createdAt, List<MemoryMediaResult> media) {

    public static MemoryResult from(Memory memory, PhotoStoragePort storage) {
        List<MemoryMediaResult> media = memory.getMedia().stream()
                .map(item -> new MemoryMediaResult(item.getId(), item.getType(),
                        storage.getAccessUrl(item.getStorageKey()), item.getDurationMs(), item.getDisplayOrder()))
                .toList();
        return new MemoryResult(memory.getId(), memory.getGroupId(), memory.getAuthorUserId(),
                memory.getAuthorName(), memory.getAuthorRelation(), memory.getTextContent(),
                memory.getVisibility(), memory.getModerationStatus(), memory.isPinned(), memory.getCreatedAt(), media);
    }
}
