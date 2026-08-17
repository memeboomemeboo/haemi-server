package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.FamilyGroupMember;
import com.memeboo2.haemi.m0.domain.model.GroupMemberStatus;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FamilyGroupRepositoryAdapter implements FamilyGroupRepository {

    private final JpaFamilyGroupRepository groups;
    private final JpaFamilyGroupMemberRepository members;

    @Override
    public FamilyGroup save(FamilyGroup group) {
        return groups.save(group);
    }

    @Override
    public Optional<FamilyGroup> findById(UUID groupId) {
        return groups.findById(groupId);
    }

    @Override
    public Optional<FamilyGroup> findByIdForUpdate(UUID groupId) {
        return groups.findByIdForUpdate(groupId);
    }

    @Override
    public Optional<FamilyGroup> findByOwnerMemberId(UUID ownerMemberId) {
        return groups.findByOwnerMemberId(ownerMemberId);
    }

    @Override
    public Optional<FamilyGroup> findActiveByMemberId(UUID memberId) {
        return members.findByMemberIdAndStatus(memberId, GroupMemberStatus.ACTIVE)
                .map(FamilyGroupMember::getGroup);
    }

    @Override
    public boolean existsActiveMembershipByMemberId(UUID memberId) {
        return members.existsByMemberIdAndStatus(memberId, GroupMemberStatus.ACTIVE);
    }
}
