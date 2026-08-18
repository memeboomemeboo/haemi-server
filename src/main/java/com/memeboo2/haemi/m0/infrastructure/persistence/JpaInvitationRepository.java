package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.Invitation;
import com.memeboo2.haemi.m0.domain.model.InvitationKind;
import com.memeboo2.haemi.m0.domain.model.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaInvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByToken(String token);
    long countByGroupIdAndStatus(UUID groupId, InvitationStatus status);
    Optional<Invitation> findByCodeAndKindAndStatus(String code, InvitationKind kind, InvitationStatus status);
}
