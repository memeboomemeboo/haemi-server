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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ownership_transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnershipTransfer {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "requested_by_member_id", nullable = false, columnDefinition = "uuid")
    private UUID requestedByMemberId;

    @Column(name = "recipient_member_id", nullable = false, columnDefinition = "uuid")
    private UUID recipientMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OwnershipTransferStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public static OwnershipTransfer request(UUID groupId, UUID requesterId, UUID recipientId) {
        OwnershipTransfer transfer = new OwnershipTransfer();
        transfer.id = UUID.randomUUID();
        transfer.groupId = groupId;
        transfer.requestedByMemberId = requesterId;
        transfer.recipientMemberId = recipientId;
        transfer.status = OwnershipTransferStatus.PENDING;
        transfer.createdAt = LocalDateTime.now();
        transfer.expiresAt = transfer.createdAt.plusHours(72);
        return transfer;
    }

    public void accept(UUID actorId) {
        expireIfNeeded();
        if (!recipientMemberId.equals(actorId)) {
            throw new M0AccessDeniedException("권한 이양 대상만 수락할 수 있어요.");
        }
        if (status != OwnershipTransferStatus.PENDING) {
            throw new M0ValidationException("수락할 수 없는 권한 이양이에요.");
        }
        status = OwnershipTransferStatus.ACCEPTED;
        acceptedAt = LocalDateTime.now();
    }

    private void expireIfNeeded() {
        if (status == OwnershipTransferStatus.PENDING && LocalDateTime.now().isAfter(expiresAt)) {
            status = OwnershipTransferStatus.EXPIRED;
        }
    }
}
