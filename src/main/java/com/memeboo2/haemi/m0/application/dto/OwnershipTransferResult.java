package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.OwnershipTransfer;
import com.memeboo2.haemi.m0.domain.model.OwnershipTransferStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OwnershipTransferResult(UUID transferId, UUID recipientMemberId,
                                      OwnershipTransferStatus status, LocalDateTime expiresAt) {
    public static OwnershipTransferResult from(OwnershipTransfer transfer) {
        return new OwnershipTransferResult(transfer.getId(), transfer.getRecipientMemberId(),
                transfer.getStatus(), transfer.getExpiresAt());
    }
}
