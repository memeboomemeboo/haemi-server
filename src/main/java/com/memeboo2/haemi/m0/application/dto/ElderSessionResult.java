package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.ElderSession;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 어르신 기기에 내려주는 세션 정보.
 * 리포트·점수 등 어르신 비노출 정보는 어떤 형태로도 포함하지 않는다.
 */
public record ElderSessionResult(
        UUID elderId,
        UUID groupId,
        String elderName,
        String accessToken,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
    public static ElderSessionResult of(ElderSession session, String elderName,
                                        String accessToken, String refreshToken) {
        return new ElderSessionResult(session.getElderId(), session.getGroupId(), elderName,
                accessToken, refreshToken, session.getRollingExpiresAt());
    }
}
