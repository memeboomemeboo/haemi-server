package com.memeboo2.haemi.notification.infrastructure;

import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * FCM 자격증명이 없는 개발/테스트 환경용 폴백 (#80). 발송 대신 로그만 남긴다.
 */
@Slf4j
public class LoggingPushSenderAdapter implements PushSenderPort {

    @Override
    public PushSendResult send(List<String> tokens, PushMessage message) {
        log.info("[PUSH-LOG] tokens={} | {} | {}", tokens.size(), message.title(), message.body());
        return new PushSendResult(tokens.size(), 0, List.of());
    }
}
