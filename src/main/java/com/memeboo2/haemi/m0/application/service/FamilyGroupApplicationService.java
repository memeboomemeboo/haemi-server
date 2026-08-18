package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.application.command.CreateFamilyGroupCommand;
import com.memeboo2.haemi.m0.application.command.CreateInvitationCommand;
import com.memeboo2.haemi.m0.application.dto.FamilyGroupResult;
import com.memeboo2.haemi.m0.application.dto.InvitationResult;
import com.memeboo2.haemi.m0.application.dto.OwnershipTransferResult;
import com.memeboo2.haemi.m0.domain.event.FamilyMemberJoinedEvent;
import com.memeboo2.haemi.m0.domain.model.*;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.InvitationRepository;
import com.memeboo2.haemi.m0.domain.repository.OwnershipTransferRepository;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyGroupApplicationService {

    private final FamilyGroupRepository groups;
    private final InvitationRepository invitations;
    private final OwnershipTransferRepository ownershipTransfers;
    private final ElderRepository elders;
    private final MemberRepository members;
    private final ApplicationEventPublisher eventPublisher;

    public FamilyGroupResult create(UUID ownerId, CreateFamilyGroupCommand command) {
        if (groups.existsActiveMembershipByMemberId(ownerId)) {
            throw new M0ConflictException("이미 참여 중인 가족 그룹이 있어요.");
        }
        if (command.relation() == null) {
            throw new M0ValidationException("어르신과의 관계는 필수예요.");
        }
        FamilyGroup group = FamilyGroup.create(ownerId, command.relation(),
                command.notificationPreference() == null ? NotificationPreference.ALL : command.notificationPreference());
        return FamilyGroupResult.from(groups.save(group));
    }

    public InvitationResult invite(UUID actorId, UUID groupId, CreateInvitationCommand command) {
        FamilyGroup group = loadGroup(groupId);
        group.requireOwner(actorId);
        if (command.kind() == InvitationKind.ELDER) {
            return InvitationResult.from(invitations.save(inviteElder(groupId, actorId)));
        }
        if (command.email() == null || command.email().isBlank() || command.relation() == null) {
            throw new M0ValidationException("초대 대상 이메일과 관계는 필수예요.");
        }
        group.reserveInvitationSlot(invitations.countPendingByGroupId(groupId));
        Invitation invitation = Invitation.create(groupId, actorId, hashEmail(command.email()), command.relation());
        return InvitationResult.from(invitations.save(invitation));
    }

    /**
     * 어르신 초대 코드 발급 (F0-01-E). 어르신은 그룹당 1인 프로필이라 member 정원(10명)에
     * 포함되지 않으므로 초대 슬롯을 예약하지 않는다.
     * 어르신 프로필이 없어도 코드를 발급할 수 있다. 프로필은 accept-elder 단계에서 자동 생성된다.
     */
    private Invitation inviteElder(UUID groupId, UUID actorId) {
        // 코드 공간이 좁으므로 미수락 코드와 겹치지 않을 때까지 다시 뽑는다.
        for (int attempt = 0; attempt < 10; attempt++) {
            Invitation candidate = Invitation.createForElder(groupId, actorId);
            if (invitations.findPendingElderInvitationByCode(candidate.getCode()).isEmpty()) {
                return candidate;
            }
        }
        throw new M0ConflictException("초대 코드를 만들지 못했어요. 잠시 후 다시 시도해주세요.");
    }

    public FamilyGroupResult acceptInvitation(UUID actorId, String token) {
        Invitation invitation = invitations.findByToken(token)
                .orElseThrow(() -> new M0NotFoundException("초대 링크"));
        if (invitation.isElderInvitation()) {
            // 어르신 초대는 accept-elder 경로로만 처리한다(F0-01-E).
            throw new M0NotFoundException("초대 링크");
        }
        if (groups.existsActiveMembershipByMemberId(actorId)) {
            throw new M0ConflictException("이미 참여 중인 가족 그룹이 있어요.");
        }
        var member = members.findById(actorId).orElseThrow(() -> new M0NotFoundException("회원"));
        if (!member.isEmailVerified() || !hashEmail(member.getEmail()).equals(invitation.getInviteeEmailHash())) {
            throw new M0ValidationException("초대받은 이메일을 확인한 계정으로만 수락할 수 있어요.");
        }
        FamilyGroup group = loadGroup(invitation.getGroupId());
        invitation.accept();
        group.acceptInvitation(actorId, invitation.getRelation());
        invitations.save(invitation);
        FamilyGroupResult result = FamilyGroupResult.from(groups.save(group));
        eventPublisher.publishEvent(new FamilyMemberJoinedEvent(invitation.getGroupId(), actorId));
        return result;
    }

    public FamilyGroupResult changeMemberRole(UUID actorId, UUID groupId, UUID memberId, GroupMemberRole role) {
        FamilyGroup group = loadGroup(groupId);
        group.changeMemberRole(actorId, memberId, role);
        return FamilyGroupResult.from(groups.save(group));
    }

    public void removeMember(UUID actorId, UUID groupId, UUID memberId) {
        FamilyGroup group = loadGroup(groupId);
        group.removeMember(actorId, memberId);
        groups.save(group);
    }

    public OwnershipTransferResult requestOwnershipTransfer(UUID actorId, UUID groupId, UUID recipientMemberId) {
        FamilyGroup group = loadGroup(groupId);
        group.requireOwner(actorId);
        if (actorId.equals(recipientMemberId) || !group.isActiveMember(recipientMemberId)) {
            throw new M0ValidationException("권한을 이양할 가족 구성원을 선택해주세요.");
        }
        OwnershipTransfer transfer = OwnershipTransfer.request(groupId, actorId, recipientMemberId);
        return OwnershipTransferResult.from(ownershipTransfers.save(transfer));
    }

    public FamilyGroupResult acceptOwnershipTransfer(UUID actorId, UUID groupId, UUID transferId) {
        OwnershipTransfer transfer = ownershipTransfers.findById(transferId)
                .orElseThrow(() -> new M0NotFoundException("권한 이양 요청"));
        if (!transfer.getGroupId().equals(groupId)) {
            throw new M0ValidationException("가족 그룹과 권한 이양 요청이 일치하지 않아요.");
        }
        FamilyGroup group = loadGroup(groupId);
        transfer.accept(actorId);
        group.transferOwnership(actorId);
        ownershipTransfers.save(transfer);
        return FamilyGroupResult.from(groups.save(group));
    }

    @Transactional(readOnly = true)
    public FamilyGroupResult get(UUID actorId, UUID groupId) {
        FamilyGroup group = loadGroup(groupId);
        group.requireActiveMember(actorId);
        return FamilyGroupResult.from(group);
    }

    private FamilyGroup loadGroup(UUID groupId) {
        return groups.findById(groupId).orElseThrow(() -> new M0NotFoundException("가족 그룹"));
    }

    private String hashEmail(String email) {
        try {
            String normalized = email.trim().toLowerCase();
            if (!normalized.contains("@")) {
                throw new M0ValidationException("올바른 이메일을 입력해주세요.");
            }
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (M0ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("이메일을 안전하게 처리할 수 없습니다.", e);
        }
    }
}
