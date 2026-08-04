package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.FamilyGroup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FamilyGroupResult(UUID groupId, UUID ownerMemberId, int memberCount,
                                LocalDateTime createdAt, List<GroupMemberResult> members) {
    public static FamilyGroupResult from(FamilyGroup group) {
        return new FamilyGroupResult(group.getId(), group.getOwnerMemberId(), group.getMemberCount(),
                group.getCreatedAt(), group.getActiveMembers().stream().map(GroupMemberResult::from).toList());
    }
}
