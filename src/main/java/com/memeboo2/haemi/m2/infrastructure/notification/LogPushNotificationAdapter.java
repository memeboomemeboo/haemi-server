package com.memeboo2.haemi.m2.infrastructure.notification;

import com.memeboo2.haemi.m2.domain.port.PushNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 로그 기반 푸시 알림 어댑터 (개발/테스트 환경 전용).
 * 운영 환경에서는 FCM/APNs 구현체로 교체한다.
 */
@Slf4j
@Component
public class LogPushNotificationAdapter implements PushNotificationPort {

    @Override
    public void sendToMember(String memberId, String title, String body) {
        log.info("[M2 PUSH] memberId={} | {} | {}", memberId, title, body);
    }

    @Override
    public void sendToGroup(Set<String> memberIds, String title, String body) {
        log.info("[M2 PUSH-GROUP] members={} | {} | {}", memberIds, title, body);
    }
}
