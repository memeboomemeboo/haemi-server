package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaElderSessionRepository extends JpaRepository<ElderSession, UUID> {

    Optional<ElderSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<ElderSession> findByElderIdAndDeviceIdAndRevokedAtIsNull(UUID elderId, String deviceId);

    List<ElderSession> findByElderIdAndRevokedAtIsNull(UUID elderId);

    @Query("select s from ElderSession s where s.elderId = :elderId and s.revokedAt >= :since")
    List<ElderSession> findRevokedSince(@Param("elderId") UUID elderId, @Param("since") LocalDateTime since);
}
