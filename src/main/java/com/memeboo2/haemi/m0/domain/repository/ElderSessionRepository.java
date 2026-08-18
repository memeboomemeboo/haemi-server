package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.ElderSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ElderSessionRepository {
    ElderSession save(ElderSession session);
    Optional<ElderSession> findByRefreshTokenHash(String refreshTokenHash);
    Optional<ElderSession> findActiveByElderIdAndDeviceId(UUID elderId, String deviceId);
    List<ElderSession> findActiveByElderId(UUID elderId);
    List<ElderSession> findRevokedSince(UUID elderId, LocalDateTime since);
}
