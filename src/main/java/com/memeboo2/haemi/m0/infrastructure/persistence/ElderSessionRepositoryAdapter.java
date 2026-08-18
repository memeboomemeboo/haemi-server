package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderSession;
import com.memeboo2.haemi.m0.domain.repository.ElderSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ElderSessionRepositoryAdapter implements ElderSessionRepository {

    private final JpaElderSessionRepository sessions;

    @Override
    public ElderSession save(ElderSession session) {
        return sessions.save(session);
    }

    @Override
    public Optional<ElderSession> findByRefreshTokenHash(String refreshTokenHash) {
        return sessions.findByRefreshTokenHash(refreshTokenHash);
    }

    @Override
    public Optional<ElderSession> findActiveByElderIdAndDeviceId(UUID elderId, String deviceId) {
        return sessions.findByElderIdAndDeviceIdAndRevokedAtIsNull(elderId, deviceId);
    }

    @Override
    public List<ElderSession> findActiveByElderId(UUID elderId) {
        return sessions.findByElderIdAndRevokedAtIsNull(elderId);
    }

    @Override
    public List<ElderSession> findRevokedSince(UUID elderId, LocalDateTime since) {
        return sessions.findRevokedSince(elderId, since);
    }
}
