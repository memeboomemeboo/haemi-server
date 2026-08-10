package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 발송 구현체 선택 (#80).
 * FirebaseMessaging 빈이 있으면 FCM, 없으면 로그 폴백. 조건부 애너테이션의 평가 순서에 기대지 않는다.
 */
@Slf4j
@Configuration
public class PushSenderConfig {

    @Bean
    public PushSenderPort pushSenderPort(ObjectProvider<FirebaseMessaging> firebaseMessaging,
                                         Environment environment) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            // 로컬·테스트에서는 자격증명 없이 도는 게 정상이라 기동을 막지 않는다.
            // 다만 운영에서 조용히 폴백으로 도는 건 사고다. 로그만 봐도 드러나게 한다. (#92)
            if (environment.matchesProfiles("prod")) {
                log.warn("FCM 자격증명이 없어 푸시 알림이 발송되지 않습니다. "
                        + "FIREBASE_CREDENTIALS 환경변수를 확인하세요. 지금은 로그만 남깁니다.");
            } else {
                log.info("FCM 자격증명이 없어 푸시 알림을 로그로만 남깁니다.");
            }
            return new LoggingPushSenderAdapter();
        }
        return new FcmPushSenderAdapter(messaging);
    }
}
