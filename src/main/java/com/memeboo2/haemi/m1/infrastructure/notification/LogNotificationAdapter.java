package com.memeboo2.haemi.m1.infrastructure.notification;

import com.memeboo2.haemi.m1.domain.port.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 실 서비스에서는 FCM/APNs 등으로 교체한다.
 */
@Slf4j
@Component
public class LogNotificationAdapter implements NotificationPort {

    @Override
    public void sendToMember(String memberId, String title, String body) {
        log.info("[PUSH] memberId={} | {} - {}", memberId, title, body);
    }

    @Override
    public void sendToGroup(Set<String> memberIds, String title, String body) {
        memberIds.forEach(id -> sendToMember(id, title, body));
    }
}
