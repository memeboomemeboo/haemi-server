package com.memeboo2.haemi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m4.domain.model.dashboard.AlertRecipientSetting;
import com.memeboo2.haemi.m4.domain.repository.AlertRecipientSettingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionSurfaceIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final MemberRepository members;
    private final AlertRecipientSettingRepository alertRecipientSettings;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductionSurfaceIntegrationTest(
            MockMvc mockMvc,
            TokenPort tokenPort,
            MemberRepository members,
            AlertRecipientSettingRepository alertRecipientSettings
    ) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.members = members;
        this.alertRecipientSettings = alertRecipientSettings;
    }

    @Test
    void disabledOpenApiReturnsNotFoundInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void healthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void legacyInstitutionDashboardIsNoLongerExposed() throws Exception {
        String familyToken = accessToken(MemberRole.FAMILY);
        String adminToken = accessToken(MemberRole.INSTITUTION_ADMIN);
        String path = "/api/v1/cognitive-dashboard/institutions/institution-api-test"
                + "?from=2026-06-30&to=2026-07-06";

        mockMvc.perform(get(path).header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(path).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void alertRecipientApiUsesAuthenticatedFamilyMemberAsPrimaryCaregiver() throws Exception {
        UUID familyMemberId = UUID.randomUUID();
        UUID institutionManagerId = createMember(MemberRole.INSTITUTION_ADMIN).getId();
        String token = accessToken(familyMemberId, MemberRole.FAMILY);
        String institutionToken = accessToken(institutionManagerId, MemberRole.INSTITUTION_ADMIN);
        String elderId = createElder(token, createGroup(token));

        mockMvc.perform(put("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "institutionManagerMemberIds": ["%s"]
                                }
                                """.formatted(institutionManagerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryCaregiverMemberId")
                        .value(familyMemberId.toString()));

        mockMvc.perform(get("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allRecipientMemberIds.length()").value(2));

        mockMvc.perform(get("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + institutionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.institutionManagerMemberIds[0]").value(institutionManagerId.toString()));
    }

    @Test
    void alertRecipientApiRejectsNonAdminRecipientsAndLegacyNonAdminReader() throws Exception {
        UUID familyMemberId = UUID.randomUUID();
        Member elderMember = createMember(MemberRole.ELDER);
        String familyToken = accessToken(familyMemberId, MemberRole.FAMILY);
        String elderToken = accessToken(elderMember.getId(), MemberRole.ELDER);
        String elderId = createElder(familyToken, createGroup(familyToken));

        mockMvc.perform(put("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + familyToken)
                        .contentType("application/json")
                        .content("""
                                {"institutionManagerMemberIds": ["%s"]}
                                """.formatted(elderMember.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        alertRecipientSettings.save(AlertRecipientSetting.createOrUpdate(
                null, elderId, familyMemberId.toString(), Set.of(elderMember.getId().toString())));
        mockMvc.perform(get("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + elderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void elderAccountCannotAccessFamilyAndInstitutionReports() throws Exception {
        String elderToken = accessToken(MemberRole.ELDER);

        mockMvc.perform(post("/api/v1/cognitive-dashboard/reports")
                        .header("Authorization", "Bearer " + elderToken)
                        .param("elderId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void walkRoutineApiIsSuspended() throws Exception {
        String token = accessToken(MemberRole.FAMILY);

        // F5-02 산책 기능 보류(#47): 엔드포인트가 더 이상 노출되지 않는다.
        mockMvc.perform(post("/api/v1/care/walk-routines")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "elderId": "elder-walk-api-test",
                                  "groupId": "group-walk-api-test",
                                  "morningTime": "09:00:00",
                                  "targetMinutes": 0
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    private String createGroup(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"relation\":\"DAUGHTER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("groupId").asText();
    }

    private String createElder(String token, String groupId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/elders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("groupId", groupId)
                        .contentType("application/json")
                        .content("""
                                {"name":"김해미","birthYear":1940,"gender":"FEMALE","residenceType":"HOME_WITH_FAMILY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("elderId").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String accessToken(MemberRole role) {
        return accessToken(UUID.randomUUID(), role);
    }

    private String accessToken(UUID memberId, MemberRole role) {
        return tokenPort.generateAccessToken(
                memberId,
                role.name().toLowerCase() + "@example.com",
                role
        );
    }

    private Member createMember(MemberRole role) {
        return members.save(Member.create(
                role.name().toLowerCase() + "-" + UUID.randomUUID() + "@example.com",
                "encoded-password",
                "테스트 사용자",
                role
        ));
    }
}
