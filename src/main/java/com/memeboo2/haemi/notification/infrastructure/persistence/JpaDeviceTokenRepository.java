package com.memeboo2.haemi.notification.infrastructure.persistence;

import com.memeboo2.haemi.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaDeviceTokenRepository extends JpaRepository<DeviceToken, String> {

    List<DeviceToken> findByMemberId(String memberId);

    List<DeviceToken> findByMemberIdIn(Collection<String> memberIds);

    List<DeviceToken> findByElderId(UUID elderId);

    void deleteByTokenIn(Collection<String> tokens);
}
