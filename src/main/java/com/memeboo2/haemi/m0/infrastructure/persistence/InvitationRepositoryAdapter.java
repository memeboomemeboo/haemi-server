package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;
import com.memeboo2.haemi.m0.domain.model.InvitationStatus;
import com.memeboo2.haemi.m0.domain.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InvitationRepositoryAdapter implements InvitationRepository {

    private final JpaInvitationRepository invitations;

    @Override
    public Invitation save(Invitation invitation) {
        return invitations.save(invitation);
    }

    @Override
    public Optional<Invitation> findByToken(String token) {
        return invitations.findByToken(token);
    }

    @Override
    public Optional<Invitation> findPendingElderInvitationByCode(String code) {
        return invitations.findByCodeAndKindAndStatus(code, InvitationKind.ELDER, InvitationStatus.PENDING);
    }

    @Override
    public long countPendingByGroupId(UUID groupId) {
        return invitations.countByGroupIdAndStatus(groupId, InvitationStatus.PENDING);
    }
}
