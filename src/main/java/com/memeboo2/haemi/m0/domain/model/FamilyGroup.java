package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "family_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FamilyGroup {

    public static final int MAX_MEMBERS = 10;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_member_id", nullable = false, columnDefinition = "uuid")
    private UUID ownerMemberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "member_count", nullable = false)
    private int memberCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FamilyGroupStatus status;

    @Column(name = "owner_hold_until")
    private LocalDateTime ownerHoldUntil;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("joinedAt ASC")
    private List<FamilyGroupMember> members = new ArrayList<>();

    public static FamilyGroup create(UUID ownerMemberId, FamilyRelation relation,
                                     NotificationPreference notificationPreference) {
        FamilyGroup group = new FamilyGroup();
        group.id = UUID.randomUUID();
        group.ownerMemberId = ownerMemberId;
        group.createdAt = LocalDateTime.now();
        group.memberCount = 1;
        group.status = FamilyGroupStatus.ACTIVE;
        group.members.add(FamilyGroupMember.owner(group, ownerMemberId, relation, notificationPreference));
        return group;
    }

    public void requireOwner(UUID actorId) {
        if (!ownerMemberId.equals(actorId)) {
            throw new M0AccessDeniedException("이 작업은 대표 보호자만 할 수 있어요.");
        }
    }

    public void requireActiveMember(UUID actorId) {
        if (findActiveMember(actorId) == null) {
            throw new M0AccessDeniedException("가족 그룹 구성원만 할 수 있는 작업이에요.");
        }
    }

    public boolean isActiveMember(UUID memberId) {
        return findActiveMember(memberId) != null;
    }

    public void reserveInvitationSlot(long pendingInvitationCount) {
        if (memberCount + pendingInvitationCount >= MAX_MEMBERS) {
            throw new M0ValidationException("최대 10명까지 함께할 수 있어요.");
        }
    }

    public void acceptInvitation(UUID memberId, FamilyRelation relation) {
        if (findActiveMember(memberId) != null) {
            throw new M0ConflictException("이미 가족 그룹에 참여하고 있어요.");
        }
        if (memberCount >= MAX_MEMBERS) {
            throw new M0ValidationException("최대 10명까지 함께할 수 있어요.");
        }
        FamilyGroupMember removedMember = findRemovedMember(memberId);
        if (removedMember != null) {
            removedMember.rejoin(relation);
        } else {
            members.add(FamilyGroupMember.member(this, memberId, relation));
        }
        memberCount++;
    }

    public void changeMemberRole(UUID actorId, UUID memberId, GroupMemberRole role) {
        requireOwner(actorId);
        FamilyGroupMember member = requireActiveMemberEntity(memberId);
        if (memberId.equals(ownerMemberId) && role != GroupMemberRole.OWNER) {
            throw new M0ValidationException("대표 보호자 권한은 이양 절차로만 변경할 수 있어요.");
        }
        if (!memberId.equals(ownerMemberId) && role == GroupMemberRole.OWNER) {
            throw new M0ValidationException("대표 보호자 권한은 이양 절차로만 변경할 수 있어요.");
        }
        member.changeRole(role);
    }

    public void removeMember(UUID actorId, UUID memberId) {
        requireOwner(actorId);
        if (ownerMemberId.equals(memberId)) {
            throw new M0ValidationException("대표 보호자는 먼저 권한을 이양해야 해요.");
        }
        FamilyGroupMember member = requireActiveMemberEntity(memberId);
        member.remove();
        memberCount--;
    }

    public void transferOwnership(UUID newOwnerMemberId) {
        FamilyGroupMember currentOwner = requireActiveMemberEntity(ownerMemberId);
        FamilyGroupMember newOwner = requireActiveMemberEntity(newOwnerMemberId);
        currentOwner.changeRole(GroupMemberRole.MEMBER);
        newOwner.changeRole(GroupMemberRole.OWNER);
        ownerMemberId = newOwnerMemberId;
        status = FamilyGroupStatus.ACTIVE;
        ownerHoldUntil = null;
    }

    /** 회원 탈퇴 시 가장 오래 함께한 가족에게 자동 승계한다. */
    public void handleOwnerWithdrawal(UUID withdrawnOwnerId) {
        if (!ownerMemberId.equals(withdrawnOwnerId)) {
            return;
        }
        FamilyGroupMember successor = members.stream()
                .filter(member -> member.isActive() && !member.getMemberId().equals(withdrawnOwnerId))
                .min(Comparator.comparing(FamilyGroupMember::getJoinedAt))
                .orElse(null);
        if (successor != null) {
            transferOwnership(successor.getMemberId());
            requireActiveMemberEntity(withdrawnOwnerId).remove();
            memberCount--;
            return;
        }

        requireActiveMemberEntity(withdrawnOwnerId).remove();
        memberCount--;
        status = FamilyGroupStatus.OWNER_HOLD;
        ownerHoldUntil = LocalDateTime.now().plusDays(30);
    }

    public List<FamilyGroupMember> getActiveMembers() {
        return members.stream().filter(FamilyGroupMember::isActive).toList();
    }

    private FamilyGroupMember requireActiveMemberEntity(UUID memberId) {
        FamilyGroupMember member = findActiveMember(memberId);
        if (member == null) {
            throw new M0NotFoundException("가족 구성원");
        }
        return member;
    }

    private FamilyGroupMember findActiveMember(UUID memberId) {
        return members.stream()
                .filter(member -> member.getMemberId().equals(memberId) && member.isActive())
                .findFirst()
                .orElse(null);
    }

    private FamilyGroupMember findRemovedMember(UUID memberId) {
        return members.stream()
                .filter(member -> member.getMemberId().equals(memberId) && !member.isActive())
                .findFirst()
                .orElse(null);
    }
}
