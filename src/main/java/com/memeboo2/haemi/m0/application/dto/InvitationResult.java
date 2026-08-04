package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.model.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResult(UUID invitationId, String token, InvitationStatus status, LocalDateTime expiresAt) {
    public static InvitationResult from(Invitation invitation) {
        return new InvitationResult(invitation.getId(), invitation.getToken(), invitation.getStatus(),
                invitation.getExpiresAt());
    }
}
