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

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "inviter_member_id", nullable = false, columnDefinition = "uuid")
    private UUID inviterMemberId;

    @Column(name = "invitee_phone_hash", nullable = false, length = 64)
    private String inviteePhoneHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FamilyRelation relation;

    @Column(nullable = false, unique = true, length = 96)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public static Invitation create(UUID groupId, UUID inviterMemberId, String inviteePhoneHash,
                                    FamilyRelation relation) {
        Invitation invitation = new Invitation();
        invitation.id = UUID.randomUUID();
        invitation.groupId = groupId;
        invitation.inviterMemberId = inviterMemberId;
        invitation.inviteePhoneHash = inviteePhoneHash;
        invitation.relation = relation;
        invitation.token = newToken();
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
}
