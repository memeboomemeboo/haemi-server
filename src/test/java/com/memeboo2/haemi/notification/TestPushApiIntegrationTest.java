package com.memeboo2.haemi.notification;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.m0.domain.model.Elder;
import com.memeboo2.haemi.m0.domain.model.FamilyGroup;
import com.memeboo2.haemi.m0.domain.model.FamilyRelation;
import com.memeboo2.haemi.m0.domain.model.Gender;
import com.memeboo2.haemi.m0.domain.model.NotificationPreference;
import com.memeboo2.haemi.m0.domain.model.ResidenceType;
import com.memeboo2.haemi.m0.domain.repository.ElderRepository;
import com.memeboo2.haemi.m0.domain.repository.FamilyGroupRepository;
import com.memeboo2.haemi.notification.application.DeviceTokenService;
import com.memeboo2.haemi.notification.domain.DevicePlatform;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.domain.port.PushSenderPort;
import com.memeboo2.haemi.notification.domain.repository.DeviceTokenRepository;
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
import static org.assertj.core.api.Assertions.assertThat;
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
    private final FamilyGroupRepository familyGroups;
    private final ElderRepository elders;
    private final DeviceTokenRepository deviceTokens;

    @Autowired
    TestPushApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, DeviceTokenService deviceTokenService,
                               FamilyGroupRepository familyGroups, ElderRepository elders,
                               DeviceTokenRepository deviceTokens) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.deviceTokenService = deviceTokenService;
        this.familyGroups = familyGroups;
        this.elders = elders;
        this.deviceTokens = deviceTokens;
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
    @DisplayName("기기는 있는데 전부 실패하면 '기기 없음'이 아니라 실패로 안내한다")
    void distinguishesFailureFromNoDevice() throws Exception {
        UUID memberId = UUID.randomUUID();
        String token = "fcm-" + UUID.randomUUID();
        deviceTokenService.register(memberId.toString(), token, DevicePlatform.ANDROID);
        when(pushSender.send(anyList(), any())).thenReturn(new PushSendResult(0, 1, List.of(token)));

        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .header("Authorization", "Bearer " + accessToken(memberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failureCount").value(1))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("발송에 실패했어요")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("정리했어요")));
    }

    @Test
    @DisplayName("인증 없이는 호출할 수 없다")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/device-tokens/test-send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"body\":\"본문\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("보호자 계정으로 어르신 폰 토큰을 등록하면 어르신 수신 대상으로 연결된다")
    void registersCaregiverOwnedTokenForElderDevice() throws Exception {
        UUID caregiverId = UUID.randomUUID();
        FamilyGroup group = familyGroups.save(FamilyGroup.create(
                caregiverId, FamilyRelation.DAUGHTER, NotificationPreference.ALL));
        Elder elder = elders.save(Elder.create(
                group.getId(), null, "김해미", 1940, Gender.FEMALE, ResidenceType.HOME_ALONE));
        String token = "fcm-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + accessToken(caregiverId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"platform\":\"ANDROID\",\"elderId\":\"%s\"}"
                                .formatted(token, elder.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.elderId").value(elder.getId().toString()));

        assertThat(deviceTokens.findByToken(token))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getMemberId()).isEqualTo(caregiverId.toString());
                    assertThat(saved.getElderId()).isEqualTo(elder.getId());
                });
    }
}
