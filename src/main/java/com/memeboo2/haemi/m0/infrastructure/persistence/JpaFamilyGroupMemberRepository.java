package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.FamilyGroupMember;
import com.memeboo2.haemi.m0.domain.model.GroupMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaFamilyGroupMemberRepository extends JpaRepository<FamilyGroupMember, UUID> {
    boolean existsByMemberIdAndStatus(UUID memberId, GroupMemberStatus status);
}
