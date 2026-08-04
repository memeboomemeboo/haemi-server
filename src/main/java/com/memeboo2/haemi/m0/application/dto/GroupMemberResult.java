package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.FamilyGroupMember;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.GroupMemberRole;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;

import java.time.LocalDateTime;
import java.util.UUID;

public record GroupMemberResult(UUID memberId, FamilyRelation relation, GroupMemberRole role,
                                NotificationPreference notificationPreference, LocalDateTime joinedAt) {
    public static GroupMemberResult from(FamilyGroupMember member) {
        return new GroupMemberResult(member.getMemberId(), member.getRelation(), member.getRole(),
                member.getNotificationPreference(), member.getJoinedAt());
    }
}
