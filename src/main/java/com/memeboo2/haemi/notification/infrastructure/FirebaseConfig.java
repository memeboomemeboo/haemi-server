package com.memeboo2.haemi.notification.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 자격증명이 설정된 환경에서만 FirebaseApp을 초기화한다 (#80).
 * 미설정이면 빈이 없고, {@link LoggingPushSenderAdapter}가 폴백으로 동작한다.
 * 설정은 되어 있는데 값이 잘못된 경우에는 조용히 넘어가지 않고 기동을 실패시킨다.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FcmProperties.class)
@Conditional(FirebaseConfig.FcmConfiguredCondition.class)
public class FirebaseConfig {

    private static final String APP_NAME = "haemi-fcm";

    @Bean
    public FirebaseApp firebaseApp(FcmProperties properties) throws IOException {
        // 테스트 컨텍스트가 여러 번 뜨는 경우를 대비해 이름으로 재사용한다.
        return FirebaseApp.getApps().stream()
                .filter(app -> APP_NAME.equals(app.getName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(buildOptions(properties), APP_NAME));
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private FirebaseOptions buildOptions(FcmProperties properties) {
        try (InputStream credentials = openCredentials(properties.credentials())) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials));
            if (properties.projectId() != null && !properties.projectId().isBlank()) {
                builder.setProjectId(properties.projectId());
            }
            log.info("FCM 자격증명을 불러왔습니다. 푸시 알림을 FCM으로 발송합니다.");
            return builder.build();
        } catch (IOException e) {
            throw new IllegalStateException("FCM 자격증명을 읽을 수 없습니다. haemi.notification.fcm.credentials 설정을 확인하세요.", e);
        }
    }

    // JSON 원문과 파일 경로 둘 다 허용한다.
    private InputStream openCredentials(String credentials) throws IOException {
        String value = credentials.trim();
        if (value.startsWith("{")) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }
        return Files.newInputStream(Path.of(value));
    }

    static class FcmConfiguredCondition implements org.springframework.context.annotation.Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            String credentials = context.getEnvironment().getProperty("haemi.notification.fcm.credentials");
            return credentials != null && !credentials.isBlank();
        }
    }
}
