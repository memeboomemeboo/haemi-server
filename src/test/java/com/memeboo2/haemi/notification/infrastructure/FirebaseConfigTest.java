package com.memeboo2.haemi.notification.infrastructure;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FCM 자격증명 유무에 따른 기동 경로 검증 (#80).
 * 운영 배포 전에 "자격증명을 넣으면 실제로 FCM으로 뜨는가"를 여기서 확인한다.
 */
class FirebaseConfigTest {

    private static final String APP_NAME = "haemi-fcm";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of())
            .withUserConfiguration(FirebaseConfig.class, PushSenderConfig.class);

    /**
     * FirebaseApp은 JVM 전역 레지스트리라 컨텍스트를 닫아도 남는다.
     * 앞 테스트가 남긴 앱을 이름으로 재사용해버리면 검증이 무의미해지므로 매번 지운다.
     */
    @BeforeEach
    void removeExistingFirebaseApp() {
        FirebaseApp.getApps().stream()
                .filter(app -> APP_NAME.equals(app.getName()))
                .toList()
                .forEach(FirebaseApp::delete);
    }

    // GoogleCredentials가 파싱할 수 있는 최소 서비스 계정 JSON (네트워크 호출 없음)
    private String serviceAccountJson() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String privateKey = Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        return """
                {
                  "type": "service_account",
                  "project_id": "haemi-test",
                  "private_key_id": "test-key-id",
                  "private_key": "-----BEGIN PRIVATE KEY-----\\n%s\\n-----END PRIVATE KEY-----\\n",
                  "client_email": "haemi-test@haemi-test.iam.gserviceaccount.com",
                  "client_id": "1234567890",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(privateKey);
    }

    @Test
    @DisplayName("자격증명이 없으면 FirebaseApp을 만들지 않고 로그 폴백으로 기동한다")
    void withoutCredentials() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
            assertThat(context.getBean(PushSenderPort.class)).isInstanceOf(LoggingPushSenderAdapter.class);
        });
    }

    @Test
    @DisplayName("자격증명 JSON 원문을 넣으면 FCM 어댑터로 기동한다")
    void withInlineCredentials() throws Exception {
        contextRunner
                .withPropertyValues("haemi.notification.fcm.credentials=" + serviceAccountJson().replace("\n", ""))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FirebaseMessaging.class);
                    assertThat(context.getBean(PushSenderPort.class)).isInstanceOf(FcmPushSenderAdapter.class);
                });
    }

    @Test
    @DisplayName("자격증명 파일 경로도 받아들인다")
    void withCredentialsFilePath(@TempDir Path tempDir) throws Exception {
        Path credentialsFile = tempDir.resolve("service-account.json");
        Files.writeString(credentialsFile, serviceAccountJson());

        contextRunner
                .withPropertyValues("haemi.notification.fcm.credentials=" + credentialsFile)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FirebaseMessaging.class);
                });
    }

    @Test
    @DisplayName("자격증명이 잘못되면 조용히 넘어가지 않고 기동에 실패한다")
    void invalidCredentialsFailFast() {
        contextRunner
                .withPropertyValues("haemi.notification.fcm.credentials=/no/such/service-account.json")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("컨텍스트가 여러 번 떠도 같은 이름의 FirebaseApp을 중복 초기화하지 않는다")
    void reusesFirebaseAppAcrossContexts() throws Exception {
        String credentials = serviceAccountJson().replace("\n", "");

        contextRunner.withPropertyValues("haemi.notification.fcm.credentials=" + credentials)
                .run(context -> assertThat(context).hasNotFailed());
        contextRunner.withPropertyValues("haemi.notification.fcm.credentials=" + credentials)
                .run(context -> assertThat(context).hasNotFailed());

        assertThat(FirebaseApp.getApps().stream().filter(app -> APP_NAME.equals(app.getName())))
                .hasSize(1);
    }
}
