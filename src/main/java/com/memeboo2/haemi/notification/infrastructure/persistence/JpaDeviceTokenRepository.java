package com.memeboo2.haemi.notification.infrastructure.persistence;

import com.memeboo2.haemi.notification.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaDeviceTokenRepository extends JpaRepository<DeviceToken, String> {

    List<DeviceToken> findByMemberId(String memberId);

    List<DeviceToken> findByMemberIdIn(Collection<String> memberIds);

    void deleteByTokenIn(Collection<String> tokens);
}
