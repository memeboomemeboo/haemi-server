package com.memeboo2.haemi.notification.domain;

import java.util.Map;

/**
 * 발송할 알림 한 건. data는 단말 라우팅용 부가 정보로, 개인정보를 담지 않는다.
 */
public record PushMessage(String title, String body, Map<String, String> data) {

    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static PushMessage of(String title, String body) {
        return new PushMessage(title, body, Map.of());
    }
}
