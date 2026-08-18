package com.memeboo2.haemi.m0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F0-01-E 어르신 참여 로그인과 평생 세션. */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ElderJoinApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ElderJoinApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
    }

    @Test
    void elderJoinsWithNamePhoneAndCodeThenKeepsTheSessionWithoutLoggingInAgain() throws Exception {
        String ownerToken = token(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        String elderId = createElder(ownerToken, groupId, "김해미");
        String code = issueElderCode(ownerToken, groupId);

        MvcResult joined = mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-1234-5678", code, "device-a")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.elderId").value(elderId))
                .andExpect(jsonPath("$.data.elderName").value("김해미"))
                .andReturn();
        String accessToken = json(joined).path("data").path("accessToken").asText();
        String refreshToken = json(joined).path("data").path("refreshToken").asText();

        // 발급받은 토큰은 어르신 전용 API에 그대로 쓸 수 있어야 한다.
        mockMvc.perform(get("/api/v1/elders/{elderId}/memories", elderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult refreshed = mockMvc.perform(post("/api/v1/elder-sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-a"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String rotated = json(refreshed).path("data").path("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(refreshToken);

        // 회전된 뒤의 옛 토큰과 다른 기기의 요청은 모두 거부한다.
        mockMvc.perform(post("/api/v1/elder-sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-a"}
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/elder-sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-b"}
                                """.formatted(rotated)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nameMismatchHoldsTheJoinAndWrongCodeIsRejected() throws Exception {
        String ownerToken = token(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        createElder(ownerToken, groupId, "김해미");
        String code = issueElderCode(ownerToken, groupId);

        mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("박해미", "010-1234-5679", code, "device-a")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("정보가 맞는지 확인 중이에요."));

        mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", "000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-1234-5679", "000000", "device-a")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("코드를 다시 확인해주세요."));

        // 보류는 차단이 아니다. 성함을 바로잡으면 같은 코드로 합류할 수 있다.
        mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-1234-5679", code, "device-a")))
                .andExpect(status().isCreated());
    }

    @Test
    void sameElderPhoneCannotJoinAnotherFamilyGroup() throws Exception {
        String firstOwner = token(UUID.randomUUID());
        String firstGroup = createGroup(firstOwner);
        createElder(firstOwner, firstGroup, "김해미");
        String firstCode = issueElderCode(firstOwner, firstGroup);
        mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", firstCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-2222-3333", firstCode, "device-a")))
                .andExpect(status().isCreated());

        String secondOwner = token(UUID.randomUUID());
        String secondGroup = createGroup(secondOwner);
        createElder(secondOwner, secondGroup, "김해미");
        String secondCode = issueElderCode(secondOwner, secondGroup);

        mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", secondCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-2222-3333", secondCode, "device-b")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 참여 중인 가족이 있어요."));
    }

    @Test
    void bereavementClosesTheElderSessionAndOwnerCanResetTheDevice() throws Exception {
        String ownerToken = token(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        String elderId = createElder(ownerToken, groupId, "김해미");
        String code = issueElderCode(ownerToken, groupId);
        MvcResult joined = mockMvc.perform(post("/api/v1/invitations/{code}/accept-elder", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody("김해미", "010-4444-5555", code, "device-a")))
                .andExpect(status().isCreated())
                .andReturn();
        String refreshToken = json(joined).path("data").path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/elders/{elderId}/bereavement/request", elderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/elders/{elderId}/bereavement/confirm", elderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // EX-F005-06: 기기 잠금 명령이 도달하기 전에도 서버가 세션을 닫는다.
        mockMvc.perform(post("/api/v1/elder-sessions/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s","deviceId":"device-a"}
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/elders/{elderId}/sessions", elderId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void elderInvitationIsSeparateFromTheFamilyLinkPath() throws Exception {
        String ownerToken = token(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        createElder(ownerToken, groupId, "김해미");

        MvcResult invitation = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"ELDER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andReturn();
        JsonNode data = json(invitation).path("data");
        assertThat(data.path("code").asText()).hasSize(6);

        // 어르신 초대는 가족 초대 수락 경로로 소비할 수 없다.
        mockMvc.perform(post("/api/v1/invitations/{token}/accept", data.path("code").asText())
                        .header("Authorization", "Bearer " + token(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    private String joinBody(String name, String phone, String code, String deviceId) {
        return """
                {"name":"%s","phoneNumber":"%s","code":"%s","deviceId":"%s"}
                """.formatted(name, phone, code, deviceId);
    }

    private String issueElderCode(String ownerToken, String groupId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\":\"ELDER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("code").asText();
    }

    private String createGroup(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"SON\",\"notificationPreference\":\"ALL\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("groupId").asText();
    }

    private String createElder(String token, String groupId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/elders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("groupId", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","birthYear":1940,"gender":"FEMALE","residenceType":"HOME_WITH_FAMILY"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("elderId").asText();
    }

    private String token(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "family-%s@example.com".formatted(memberId), MemberRole.FAMILY);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
