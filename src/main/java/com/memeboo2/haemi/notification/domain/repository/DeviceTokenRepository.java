package com.memeboo2.haemi.notification.domain.repository;

import com.memeboo2.haemi.notification.domain.DeviceToken;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository {

    DeviceToken save(DeviceToken deviceToken);

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByMemberId(UUID memberId);

    List<DeviceToken> findByMemberIds(Collection<UUID> memberIds);

    List<DeviceToken> findByElderId(UUID elderId);

    void deleteByToken(String token);

    // FCM이 영구 실패로 응답한 토큰 정리
    void deleteAllByTokens(Collection<String> tokens);
}
