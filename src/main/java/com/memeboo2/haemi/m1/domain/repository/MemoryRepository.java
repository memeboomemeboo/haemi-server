package com.memeboo2.haemi.m1.domain.repository;

import com.memeboo2.haemi.m1.domain.model.memory.Memory;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository {
    Memory save(Memory memory);
    Optional<Memory> findById(UUID memoryId);
    Page<Memory> findFamilyFeed(UUID groupId, Pageable pageable);
    Page<Memory> findElderFeed(UUID groupId, Pageable pageable);
    Page<Memory> findByGroupIdAndModerationStatus(UUID groupId, MemoryModerationStatus status, Pageable pageable);
    void delete(Memory memory);
}
