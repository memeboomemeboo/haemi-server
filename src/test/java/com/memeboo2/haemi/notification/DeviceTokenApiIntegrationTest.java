package com.memeboo2.haemi.notification;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.notification.domain.DeviceToken;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import com.memeboo2.haemi.notification.infrastructure.LoggingPushSenderAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceTokenApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final DeviceTokenRepository deviceTokens;
    private final PushSenderPort pushSender;

    @Autowired
    DeviceTokenApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort,
                                  DeviceTokenRepository deviceTokens, PushSenderPort pushSender) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.deviceTokens = deviceTokens;
        this.pushSender = pushSender;
    }

    private String accessToken(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "member-%s@haemi.test".formatted(memberId), MemberRole.FAMILY);
    }

    private String registerBody(String token) {
        return "{\"token\":\"%s\",\"platform\":\"ANDROID\"}".formatted(token);
    }

    @Test
    @DisplayName("자격증명이 없는 환경에서는 로그 폴백 sender가 뜬다")
    void fallsBackToLoggingSenderWithoutCredentials() {
        assertThat(pushSender).isInstanceOf(LoggingPushSenderAdapter.class);
    }

    @Test
    @DisplayName("토큰을 등록하면 내 목록에서 조회된다")
    void registerAndList() throws Exception {
        UUID memberId = UUID.randomUUID();
        String jwt = accessToken(memberId);
        String fcmToken = "fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(fcmToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value(fcmToken))
                .andExpect(jsonPath("$.data.platform").value("ANDROID"));

        mockMvc.perform(get("/api/v1/device-tokens").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].token").value(fcmToken));
    }

    @Test
    @DisplayName("같은 토큰을 다른 계정으로 재등록하면 소유자가 이전된다")
    void reRegisterTransfersOwnership() throws Exception {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        String fcmToken = "fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(firstOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(fcmToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(secondOwner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(fcmToken)))
                .andExpect(status().isOk());

        // 토큰은 한 건으로 유지되고 소유자만 바뀐다.
        assertThat(deviceTokens.findByMemberId(firstOwner.toString())).isEmpty();
        assertThat(deviceTokens.findByMemberId(secondOwner.toString()))
                .extracting(DeviceToken::getToken)
                .containsExactly(fcmToken);
    }

    @Test
    @DisplayName("남의 토큰은 해지할 수 없다")
    void unregisterOtherMembersTokenIsForbidden() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        String fcmToken = "fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(fcmToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(fcmToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(deviceTokens.findByToken(fcmToken)).isPresent();
    }

    @Test
    @DisplayName("본인 토큰 해지는 저장소에서도 사라진다")
    void unregisterOwnToken() throws Exception {
        UUID memberId = UUID.randomUUID();
        String jwt = accessToken(memberId);
        String fcmToken = "fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(fcmToken)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\"}".formatted(fcmToken)))
                .andExpect(status().isOk());

        assertThat(deviceTokens.findByToken(fcmToken)).isEmpty();
    }

    @Test
    @DisplayName("인증 없이는 토큰을 등록할 수 없다")
    void registrationRequiresAuthentication() throws Exception {
        // 이 앱의 시큐리티 체인은 미인증 요청에 401이 아니라 403을 돌려준다(앱 공통 동작).
        mockMvc.perform(post("/api/v1/device-tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("fcm-anonymous")))
                .andExpect(status().isForbidden());

        assertThat(deviceTokens.findByToken("fcm-anonymous")).isEmpty();
    }

    @Test
    @DisplayName("토큰이 비어 있으면 400으로 거절한다")
    void blankTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\",\"platform\":\"ANDROID\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
