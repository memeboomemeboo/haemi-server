package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaFamilyGroupRepository extends JpaRepository<FamilyGroup, UUID> {
    java.util.Optional<FamilyGroup> findByOwnerMemberId(UUID ownerMemberId);
}
