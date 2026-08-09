package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.Invitation;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findByToken(String token);
    long countPendingByGroupId(UUID groupId);
}
