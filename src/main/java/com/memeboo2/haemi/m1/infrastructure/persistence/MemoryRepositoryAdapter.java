package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.memory.Memory;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;
import com.memeboo2.haemi.m1.domain.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemoryRepositoryAdapter implements MemoryRepository {

    private final JpaMemoryRepository memories;

    @Override
    public Memory save(Memory memory) {
        return memories.save(memory);
    }

    @Override
    public Optional<Memory> findById(UUID memoryId) {
        return memories.findById(memoryId);
    }

    @Override
    public Page<Memory> findFamilyFeed(UUID groupId, Pageable pageable) {
        return memories.findByGroupIdAndModerationStatus(groupId, MemoryModerationStatus.CLEAR, pageable);
    }

    @Override
    public Page<Memory> findElderFeed(UUID groupId, Pageable pageable) {
        return memories.findByGroupIdAndVisibilityAndModerationStatus(groupId, MemoryVisibility.GROUP_ALL,
                MemoryModerationStatus.CLEAR, pageable);
    }

    @Override
    public Page<Memory> findByGroupIdAndModerationStatus(UUID groupId, MemoryModerationStatus status,
                                                          Pageable pageable) {
        return memories.findByGroupIdAndModerationStatus(groupId, status, pageable);
    }

    @Override
    public void delete(Memory memory) {
        memories.delete(memory);
    }
}
