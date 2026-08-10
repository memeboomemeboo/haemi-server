package com.memeboo2.haemi.notification;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.notification.application.DeviceTokenService;
import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * dev 전용 테스트 발송 엔드포인트 (#80).
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class TestPushApiIntegrationTest {

    @MockitoBean PushSenderPort pushSender;

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final DeviceTokenService deviceTokenService;

    @Autowired
    TestPushApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, DeviceTokenService deviceTokenService) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.deviceTokenService = deviceTokenService;
    }

    private String accessToken(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "member-%s@haemi.test".formatted(memberId), MemberRole.FAMILY);
    }

    @Test
    @DisplayName("등록된 본인 기기로 발송하고 결과 건수를 돌려준다")
    void sendsToSelfAndReportsResult() throws Exception {
        UUID memberId = UUID.randomUUID();
        deviceTokenService.register(memberId.toString(), "fcm-" + UUID.randomUUID(), DevicePlatform.WEB);
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(1, 0, List.of()));

        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .header("Authorization", "Bearer " + accessToken(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }

    @Test
    @DisplayName("등록된 기기가 없으면 발송 0건과 안내 문구를 돌려준다")
    void reportsWhenNoDeviceRegistered() throws Exception {
        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .header("Authorization", "Bearer " + accessToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("발송된 기기가 없어요")));
    }

    @Test
    @DisplayName("인증 없이는 호출할 수 없다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isForbidden());
    }
}
