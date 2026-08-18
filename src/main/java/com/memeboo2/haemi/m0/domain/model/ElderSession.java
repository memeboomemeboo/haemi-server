package com.memeboo2.haemi.m0.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 평생 세션 (F0-01-E).
 *
 * <p>기기에 바인딩한 refresh token을 rolling으로 연장해, 매일 쓰는 한 재로그인을 요구하지 않는다.
 * 어르신 화면에는 로그아웃 UI가 없으므로 폐기는 owner 경로 또는 상태 변경(F0-05)으로만 일어난다.
 */
@Entity
@Table(name = "elder_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderSession {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false, columnDefinition = "uuid")
    private UUID elderId;

    @Column(name = "group_id", nullable = false, columnDefinition = "uuid")
    private UUID groupId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "last_refreshed_at", nullable = false)
    private LocalDateTime lastRefreshedAt;

    @Column(name = "rolling_expires_at", nullable = false)
    private LocalDateTime rollingExpiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 40)
    private ElderSessionRevokeReason revokedReason;

    public static ElderSession issue(UUID elderId, UUID groupId, String deviceId, String refreshTokenHash,
                                     LocalDateTime now, long rollingDays) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new M0ValidationException("기기 정보가 필요해요.");
        }
        ElderSession session = new ElderSession();
        session.id = UUID.randomUUID();
        session.elderId = elderId;
        session.groupId = groupId;
        session.deviceId = deviceId.trim();
        session.refreshTokenHash = refreshTokenHash;
        session.issuedAt = now;
        session.lastRefreshedAt = now;
        session.rollingExpiresAt = now.plusDays(rollingDays);
        return session;
    }

    /** 사용할 때마다 만료를 다시 밀어준다. 매일 사용하는 한 사실상 무기한 유지된다. */
    public void rotate(String newRefreshTokenHash, LocalDateTime now, long rollingDays) {
        this.refreshTokenHash = newRefreshTokenHash;
        this.lastRefreshedAt = now;
        this.rollingExpiresAt = now.plusDays(rollingDays);
    }

    public void revoke(ElderSessionRevokeReason reason, LocalDateTime now) {
        if (revokedAt != null) {
            return;
        }
        this.revokedAt = now;
        this.revokedReason = reason;
    }

    /** 사별 오등록 복구(48시간 내)에서만 되살린다. 어르신에게 재로그인을 요구하지 않기 위해서다. */
    public void restore(LocalDateTime now, long rollingDays) {
        this.revokedAt = null;
        this.revokedReason = null;
        this.lastRefreshedAt = now;
        this.rollingExpiresAt = now.plusDays(rollingDays);
    }

    public boolean isUsableAt(LocalDateTime now) {
        return revokedAt == null && now.isBefore(rollingExpiresAt);
    }

    public boolean matchesDevice(String candidateDeviceId) {
        return candidateDeviceId != null && deviceId.equals(candidateDeviceId.trim());
    }
}
