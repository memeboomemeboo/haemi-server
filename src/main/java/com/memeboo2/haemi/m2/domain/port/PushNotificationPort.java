package com.memeboo2.haemi.m2.domain.port;

import java.util.Set;

/**
 * M2 전용 푸시 알림 포트.
 * 실 서비스에서는 FCM/APNs로 교체한다.
 */
public interface PushNotificationPort {

    void sendToMember(String memberId, String title, String body);

    void sendToGroup(Set<String> memberIds, String title, String body);
}
