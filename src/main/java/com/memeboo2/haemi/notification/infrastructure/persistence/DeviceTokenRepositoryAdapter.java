package com.memeboo2.haemi.notification.infrastructure.persistence;

import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceTokenRepositoryAdapter implements DeviceTokenRepository {

    private final JpaDeviceTokenRepository jpa;

    @Override
    public DeviceToken save(DeviceToken deviceToken) {
        return jpa.save(deviceToken);
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return jpa.findById(token);
    }

    @Override
    public List<DeviceToken> findByMemberId(String memberId) {
        return jpa.findByMemberId(memberId);
    }

    @Override
    public List<DeviceToken> findByMemberIds(Collection<String> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return jpa.findByMemberIdIn(memberIds);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        jpa.deleteById(token);
    }

    @Override
    @Transactional
    public void deleteAllByTokens(Collection<String> tokens) {
        if (tokens.isEmpty()) {
            return;
        }
        jpa.deleteByTokenIn(tokens);
    }
}
