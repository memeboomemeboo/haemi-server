package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Elder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaElderRepository extends JpaRepository<Elder, UUID> {
    Optional<Elder> findByGroupId(UUID groupId);
    boolean existsByGroupId(UUID groupId);
}
