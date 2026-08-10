package com.memeboo2.haemi.notification.domain.port;

import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;

import java.util.List;

/**
 * 실제 전송 경계. 운영은 FCM, 자격증명이 없는 환경은 로그 구현체가 뜬다.
 */
public interface PushSenderPort {

    PushSendResult send(List<String> tokens, PushMessage message);
}
