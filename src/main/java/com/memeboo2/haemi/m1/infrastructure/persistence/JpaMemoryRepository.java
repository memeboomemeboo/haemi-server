package com.memeboo2.haemi.m1.infrastructure.persistence;

import com.memeboo2.haemi.m1.domain.model.memory.Memory;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryModerationStatus;
import com.memeboo2.haemi.m1.domain.model.memory.MemoryVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMemoryRepository extends JpaRepository<Memory, UUID> {

    Page<Memory> findByGroupIdAndModerationStatus(UUID groupId, MemoryModerationStatus status, Pageable pageable);

    Page<Memory> findByGroupIdAndVisibilityAndModerationStatus(UUID groupId, MemoryVisibility visibility,
                                                                MemoryModerationStatus status, Pageable pageable);
}
