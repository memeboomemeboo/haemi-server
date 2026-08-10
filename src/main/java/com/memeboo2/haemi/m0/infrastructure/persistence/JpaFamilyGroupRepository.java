package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.UUID;

public interface JpaFamilyGroupRepository extends JpaRepository<FamilyGroup, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select familyGroup from FamilyGroup familyGroup where familyGroup.id = :groupId")
    java.util.Optional<FamilyGroup> findByIdForUpdate(@Param("groupId") UUID groupId);

    java.util.Optional<FamilyGroup> findByOwnerMemberId(UUID ownerMemberId);
}
