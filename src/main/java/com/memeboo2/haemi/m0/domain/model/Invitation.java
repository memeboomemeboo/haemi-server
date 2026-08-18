package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 6자리 코드는 추측 가능한 공간이므로 초대 1건당 시도 횟수를 제한한다.
     * 소진되면 owner가 코드를 다시 발급해야 한다(EX-F001E-01).
     */
    public static final int MAX_CODE_ATTEMPTS = 10;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "inviter_member_id", nullable = false, columnDefinition = "uuid")
    private UUID inviterMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationKind kind;

    @Column(name = "invitee_email_hash", nullable = false, length = 64)
    private String inviteeEmailHash;

    /** 어르신 초대에는 관계가 없다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FamilyRelation relation;

    @Column(nullable = false, unique = true, length = 96)
    private String token;

    /** 어르신 초대 전용 6자리 숫자 코드. 가족 초대는 null이다. */
    @Column(length = 6)
    private String code;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    /** 성함 불일치로 합류를 보류한 시각 (EX-F001E-02). owner 확인 대상임을 표시한다. */
    @Column(name = "held_at")
    private LocalDateTime heldAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public static Invitation create(UUID groupId, UUID inviterMemberId, String inviteeEmailHash,
                                    FamilyRelation relation) {
        Invitation invitation = newInvitation(groupId, inviterMemberId, InvitationKind.FAMILY);
        invitation.inviteeEmailHash = inviteeEmailHash;
        invitation.relation = relation;
        return invitation;
    }

    /** 어르신 참여 코드 발급 (F0-01-E). 유효기간은 가족 초대와 같은 72시간이다. */
    public static Invitation createForElder(UUID groupId, UUID inviterMemberId) {
        Invitation invitation = newInvitation(groupId, inviterMemberId, InvitationKind.ELDER);
        invitation.inviteeEmailHash = "";
        invitation.code = newCode();
        return invitation;
    }

    private static Invitation newInvitation(UUID groupId, UUID inviterMemberId, InvitationKind kind) {
        Invitation invitation = new Invitation();
        invitation.id = UUID.randomUUID();
        invitation.groupId = groupId;
        invitation.inviterMemberId = inviterMemberId;
        invitation.kind = kind;
        invitation.token = newToken();
        invitation.attemptCount = 0;
        invitation.createdAt = LocalDateTime.now();
        invitation.expiresAt = invitation.createdAt.plusHours(72);
        invitation.status = InvitationStatus.PENDING;
        return invitation;
    }

    public void accept() {
        expireIfNeeded();
        if (status != InvitationStatus.PENDING) {
            throw new M0ValidationException("수락할 수 없는 초대예요.");
        }
        status = InvitationStatus.ACCEPTED;
        acceptedAt = LocalDateTime.now();
        heldAt = null;
    }

    public void revoke() {
        if (status == InvitationStatus.PENDING) {
            status = InvitationStatus.REVOKED;
        }
    }

    public boolean isPending() {
        expireIfNeeded();
        return status == InvitationStatus.PENDING;
    }

    public boolean isElderInvitation() {
        return kind == InvitationKind.ELDER;
    }

    /**
     * 어르신 합류 시도 1건을 기록한다. 시도 한도를 넘으면 코드를 폐기해 무차별 대입을 끊는다.
     * 만료·폐기된 코드는 여기서 걸러지고, 호출자는 "코드를 다시 확인해주세요"로만 응답한다.
     */
    public void recordJoinAttempt() {
        if (!isPending()) {
            throw new M0ValidationException("초대가 만료되었어요. 다시 요청해주세요.");
        }
        attemptCount++;
        if (attemptCount > MAX_CODE_ATTEMPTS) {
            revoke();
            throw new M0ValidationException("코드를 다시 확인해주세요.");
        }
    }

    /** 입력 성함이 프로필과 다를 때 합류를 보류한다. 차단이 아니라 owner 확인 대기 상태다. */
    public void hold(LocalDateTime now) {
        heldAt = now;
    }

    public boolean isHeld() {
        return heldAt != null;
    }

    private void expireIfNeeded() {
        if (status == InvitationStatus.PENDING && LocalDateTime.now().isAfter(expiresAt)) {
            status = InvitationStatus.EXPIRED;
        }
    }

    private static String newToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String newCode() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }
}
