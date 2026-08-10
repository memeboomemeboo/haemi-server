package com.memeboo2.haemi.notification.domain;

import java.util.List;

/**
 * 발송 결과. invalidTokens는 FCM이 영구 실패로 응답한 토큰만 담는다(일시 오류 제외).
 */
public record PushSendResult(int successCount, int failureCount, List<String> invalidTokens) {

    public PushSendResult {
        invalidTokens = invalidTokens == null ? List.of() : List.copyOf(invalidTokens);
    }

    public static PushSendResult empty() {
        return new PushSendResult(0, 0, List.of());
    }
}
