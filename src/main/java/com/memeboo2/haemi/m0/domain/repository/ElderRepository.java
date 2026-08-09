package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.Elder;

import java.util.Optional;
import java.util.UUID;

public interface ElderRepository {
    Elder save(Elder elder);
    Optional<Elder> findById(UUID elderId);
    Optional<Elder> findByGroupId(UUID groupId);
    boolean existsByGroupId(UUID groupId);
}
