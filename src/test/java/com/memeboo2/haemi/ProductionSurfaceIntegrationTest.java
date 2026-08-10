package com.memeboo2.haemi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProductionSurfaceIntegrationTest(MockMvc mockMvc, TokenPort tokenPort) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
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
                .andExpect(status().isForbidden());

        mockMvc.perform(get(path).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void alertRecipientApiUsesAuthenticatedFamilyMemberAsPrimaryCaregiver() throws Exception {
        UUID familyMemberId = UUID.randomUUID();
        String token = accessToken(familyMemberId, MemberRole.FAMILY);
        String elderId = createElder(token, createGroup(token));

        mockMvc.perform(put("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "institutionManagerMemberIds": ["manager-api-test"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryCaregiverMemberId")
                        .value(familyMemberId.toString()));

        mockMvc.perform(get("/api/v1/cognitive-dashboard/alerts/recipients/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allRecipientMemberIds.length()").value(2));
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
}
