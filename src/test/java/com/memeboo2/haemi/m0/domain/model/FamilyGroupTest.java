package com.memeboo2.haemi.m0.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FamilyGroupTest {

    @Test
    void acceptsAtMostTenFamilyMembersIncludingOwner() {
        FamilyGroup group = FamilyGroup.create(UUID.randomUUID(), FamilyRelation.DAUGHTER,
                NotificationPreference.ALL);

        for (int i = 0; i < 9; i++) {
            group.acceptInvitation(UUID.randomUUID(), FamilyRelation.OTHER);
        }

        assertThat(group.getMemberCount()).isEqualTo(10);
        assertThatThrownBy(() -> group.acceptInvitation(UUID.randomUUID(), FamilyRelation.OTHER))
                .isInstanceOf(M0ValidationException.class)
                .hasMessage("최대 10명까지 함께할 수 있어요.");
    }

    @Test
    void onlyOwnershipTransferCanChangeOwnerRole() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        FamilyGroup group = FamilyGroup.create(ownerId, FamilyRelation.DAUGHTER, NotificationPreference.ALL);
        group.acceptInvitation(memberId, FamilyRelation.SON);

        assertThatThrownBy(() -> group.changeMemberRole(ownerId, ownerId, GroupMemberRole.MEMBER))
                .isInstanceOf(M0ValidationException.class);
        assertThatThrownBy(() -> group.changeMemberRole(ownerId, memberId, GroupMemberRole.OWNER))
                .isInstanceOf(M0ValidationException.class);

        group.transferOwnership(memberId);

        assertThat(group.getOwnerMemberId()).isEqualTo(memberId);
    }

    @Test
    void ownerWithdrawalTransfersToOldestMemberOrPlacesGroupOnThirtyDayHold() {
        UUID ownerId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        FamilyGroup withSuccessor = FamilyGroup.create(ownerId, FamilyRelation.DAUGHTER, NotificationPreference.ALL);
        withSuccessor.acceptInvitation(successorId, FamilyRelation.SON);

        withSuccessor.handleOwnerWithdrawal(ownerId);

        assertThat(withSuccessor.getOwnerMemberId()).isEqualTo(successorId);
        assertThat(withSuccessor.getMemberCount()).isOne();
        assertThat(withSuccessor.getStatus()).isEqualTo(FamilyGroupStatus.ACTIVE);

        FamilyGroup withoutSuccessor = FamilyGroup.create(UUID.randomUUID(), FamilyRelation.DAUGHTER,
                NotificationPreference.ALL);
        withoutSuccessor.handleOwnerWithdrawal(withoutSuccessor.getOwnerMemberId());

        assertThat(withoutSuccessor.getMemberCount()).isZero();
        assertThat(withoutSuccessor.getStatus()).isEqualTo(FamilyGroupStatus.OWNER_HOLD);
        assertThat(withoutSuccessor.getOwnerHoldUntil()).isNotNull();
    }
}
