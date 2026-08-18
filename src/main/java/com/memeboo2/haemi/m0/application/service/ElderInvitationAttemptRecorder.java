package com.memeboo2.haemi.m0.application.service;

import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 참여 로그인의 시도 기록을 합류 트랜잭션과 분리해 커밋한다 (F0-01-E).
 *
 * <p>합류 실패는 예외로 끝나므로, 시도 횟수·보류 표시를 같은 트랜잭션에서 쓰면 전부 롤백되어
 * 6자리 코드의 대입 제한(EX-F001E-01)과 보류 기록(EX-F001E-02)이 남지 않는다. 그래서 이
 * 두 쓰기만 {@code REQUIRES_NEW}로 따로 커밋한다.
 */
@Component
@RequiredArgsConstructor
public class ElderInvitationAttemptRecorder {

    private final InvitationRepository invitations;

    /** @return 시도 한도 안이면 true. false면 호출자가 실패 응답을 만든다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordAttempt(UUID invitationId) {
        Invitation invitation = invitations.findById(invitationId).orElse(null);
        if (invitation == null) {
            return false;
        }
        boolean allowed = invitation.recordJoinAttempt();
        invitations.save(invitation);
        return allowed;
    }

    /** 성함 불일치로 합류를 보류한 사실을 남긴다. owner 확인 뒤 같은 코드로 다시 시도할 수 있다. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void hold(UUID invitationId, LocalDateTime now) {
        invitations.findById(invitationId).ifPresent(invitation -> {
            invitation.hold(now);
            invitations.save(invitation);
        });
    }
}
