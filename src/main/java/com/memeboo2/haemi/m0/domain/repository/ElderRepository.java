package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.Elder;

import java.util.Optional;
import java.util.UUID;

public interface ElderRepository {
    Elder save(Elder elder);
    Optional<Elder> findById(UUID elderId);
    Optional<Elder> findByGroupId(UUID groupId);
    Optional<Elder> findByMemberId(UUID memberId);
    boolean existsByGroupId(UUID groupId);
}
