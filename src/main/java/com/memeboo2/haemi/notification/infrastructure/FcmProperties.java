package com.memeboo2.haemi.notification.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM 자격증명 설정 (#80).
 * credentials는 서비스 계정 JSON 원문 또는 JSON 파일 경로 둘 다 허용한다.
 * 비어 있으면 FCM을 초기화하지 않고 로그 폴백으로 동작한다.
 */
@ConfigurationProperties(prefix = "haemi.notification.fcm")
public record FcmProperties(String credentials, String projectId) {

    public boolean isConfigured() {
        return credentials != null && !credentials.isBlank();
    }
}
