package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;
import com.memeboo2.haemi.m0.domain.model.InvitationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 가족 초대는 링크 토큰을, 어르신 초대는 6자리 코드를 담는다.
 * {@code heldAt}이 있으면 입력 성함이 프로필과 달라 합류가 보류된 건이다(EX-F001E-02).
 */
public record InvitationResult(UUID invitationId, InvitationKind kind, String token, String code,
                               InvitationStatus status, LocalDateTime expiresAt, LocalDateTime heldAt) {
    public static InvitationResult from(Invitation invitation) {
        boolean elder = invitation.isElderInvitation();
        return new InvitationResult(invitation.getId(), invitation.getKind(),
                elder ? null : invitation.getToken(), invitation.getCode(),
                invitation.getStatus(), invitation.getExpiresAt(), invitation.getHeldAt());
    }
}
