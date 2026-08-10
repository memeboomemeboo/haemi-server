package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기기 토큰 등록/해지 (#80). 토큰 소유자는 항상 인증된 사용자다.
 */
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokens;

    @Transactional
    public DeviceTokenResult register(String memberId, String token, DevicePlatform platform) {
        LocalDateTime now = LocalDateTime.now();
        // 기기 재로그인 등으로 같은 토큰이 다시 올라오면 소유자를 이전한다.
        DeviceToken deviceToken = deviceTokens.findByToken(token)
                .map(existing -> {
                    existing.refresh(memberId, platform, now);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.register(token, memberId, platform, now));
        return DeviceTokenResult.from(deviceTokens.save(deviceToken));
    }

    @Transactional
    public void unregister(String memberId, String token) {
        deviceTokens.findByToken(token).ifPresent(deviceToken -> {
            if (!deviceToken.isOwnedBy(memberId)) {
                throw new DeviceTokenAccessDeniedException();
            }
            deviceTokens.deleteByToken(token);
        });
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResult> findMyTokens(String memberId) {
        return deviceTokens.findByMemberId(memberId).stream()
                .map(DeviceTokenResult::from)
                .toList();
    }
}
