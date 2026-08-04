package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.FamilyGroup;

import java.util.Optional;
import java.util.UUID;

public interface FamilyGroupRepository {
    FamilyGroup save(FamilyGroup group);
    Optional<FamilyGroup> findById(UUID groupId);
    Optional<FamilyGroup> findByOwnerMemberId(UUID ownerMemberId);
    boolean existsActiveMembershipByMemberId(UUID memberId);
}
