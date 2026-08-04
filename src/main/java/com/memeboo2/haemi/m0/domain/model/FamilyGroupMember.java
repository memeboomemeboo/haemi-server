package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_group_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyGroupMember {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private FamilyGroup group;

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FamilyRelation relation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_preference", nullable = false, length = 30)
    private NotificationPreference notificationPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    static FamilyGroupMember owner(FamilyGroup group, UUID memberId, FamilyRelation relation,
                                   NotificationPreference notificationPreference) {
        return create(group, memberId, relation, GroupMemberRole.OWNER, notificationPreference);
    }

    static FamilyGroupMember member(FamilyGroup group, UUID memberId, FamilyRelation relation) {
        return create(group, memberId, relation, GroupMemberRole.MEMBER, NotificationPreference.ALL);
    }

    private static FamilyGroupMember create(FamilyGroup group, UUID memberId, FamilyRelation relation,
                                            GroupMemberRole role, NotificationPreference preference) {
        FamilyGroupMember member = new FamilyGroupMember();
        member.id = UUID.randomUUID();
        member.group = group;
        member.memberId = memberId;
        member.relation = relation;
        member.role = role;
        member.notificationPreference = preference;
        member.status = GroupMemberStatus.ACTIVE;
        member.joinedAt = LocalDateTime.now();
        return member;
    }

    void changeRole(GroupMemberRole role) {
        this.role = role;
    }

    void remove() {
        this.status = GroupMemberStatus.REMOVED;
        this.removedAt = LocalDateTime.now();
    }

    void rejoin(FamilyRelation relation) {
        this.relation = relation;
        this.role = GroupMemberRole.MEMBER;
        this.status = GroupMemberStatus.ACTIVE;
        this.joinedAt = LocalDateTime.now();
        this.removedAt = null;
    }

    public boolean isActive() {
        return status == GroupMemberStatus.ACTIVE;
    }
}
