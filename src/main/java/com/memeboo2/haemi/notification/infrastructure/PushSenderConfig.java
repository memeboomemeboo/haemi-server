package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 발송 구현체 선택 (#80).
 * FirebaseMessaging 빈이 있으면 FCM, 없으면 로그 폴백. 조건부 애너테이션의 평가 순서에 기대지 않는다.
 */
@Slf4j
@Configuration
public class PushSenderConfig {

    @Bean
    public PushSenderPort pushSenderPort(ObjectProvider<FirebaseMessaging> firebaseMessaging) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.info("FCM 자격증명이 없어 푸시 알림을 로그로만 남깁니다.");
            return new LoggingPushSenderAdapter();
        }
        return new FcmPushSenderAdapter(messaging);
    }
}
