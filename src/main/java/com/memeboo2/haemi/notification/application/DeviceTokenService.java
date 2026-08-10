package com.memeboo2.haemi.notification.application;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 기기 토큰 등록/해지 (#80). 토큰 소유자는 항상 인증된 사용자다.
 */
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokens;
    private final ElderDeviceAccessValidator elderDeviceAccessValidator;

    @Transactional
    public DeviceTokenResult register(UUID memberId, String token, DevicePlatform platform) {
        return register(memberId, token, platform, null);
    }

    /**
     * 토큰의 계정 소유자는 인증 주체로 고정하고, 어르신 본인 휴대전화라면 별도 수신 대상으로 연결한다.
     * Mode B에서는 보호자 계정으로 등록하더라도 elderId를 통해 어르신 기기로 발송된다.
     */
    @Transactional
    public DeviceTokenResult register(UUID memberId, String token, DevicePlatform platform, UUID elderId) {
        if (elderId != null) {
            elderDeviceAccessValidator.requireCanBind(memberId, elderId);
        }
        LocalDateTime now = LocalDateTime.now();
        // 기기 재로그인 등으로 같은 토큰이 다시 올라오면 소유자를 이전한다.
        DeviceToken deviceToken = deviceTokens.findByToken(token)
                .map(existing -> {
                    existing.refresh(memberId, platform, elderId, now);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.register(token, memberId, platform, elderId, now));
        return DeviceTokenResult.from(deviceTokens.save(deviceToken));
    }

    @Transactional
    public void unregister(UUID memberId, String token) {
        deviceTokens.findByToken(token).ifPresent(deviceToken -> {
            if (!deviceToken.isOwnedBy(memberId)) {
                throw new DeviceTokenAccessDeniedException();
            }
            deviceTokens.deleteByToken(token);
        });
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResult> findMyTokens(UUID memberId) {
        return deviceTokens.findByMemberId(memberId).stream()
                .map(DeviceTokenResult::from)
                .toList();
    }
}
