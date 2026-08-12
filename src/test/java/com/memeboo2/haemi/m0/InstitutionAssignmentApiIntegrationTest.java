package com.memeboo2.haemi.m0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstitutionAssignmentApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final MemberRepository members;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    InstitutionAssignmentApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, MemberRepository members) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.members = members;
    }

    @Test
    void institutionAdminUsesAggregatePortalAndCannotReadElderProfile() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String ownerToken = token(ownerId, MemberRole.FAMILY);
        String groupId = createGroup(ownerToken);
        String elderId = createElder(ownerToken, groupId);
        Member assignedAdmin = createInstitutionAdmin("assigned");
        Member unrelatedAdmin = createInstitutionAdmin("unrelated");

        mockMvc.perform(post("/api/v1/elders/{elderId}/institution-assignments", elderId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institutionId":"care-center-a","institutionAdminMemberId":"%s"}
                                """.formatted(assignedAdmin.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/elders/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token(assignedAdmin.getId(), MemberRole.INSTITUTION_ADMIN)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/cognitive-dashboard/metrics")
                        .queryParam("elderId", elderId)
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2026-01-07")
                        .header("Authorization", "Bearer " + token(assignedAdmin.getId(), MemberRole.INSTITUTION_ADMIN)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/elders/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token(unrelatedAdmin.getId(), MemberRole.INSTITUTION_ADMIN)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/elders/{elderId}/institution-assignments/{memberId}", elderId, assignedAdmin.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/elders/{elderId}", elderId)
                        .header("Authorization", "Bearer " + token(assignedAdmin.getId(), MemberRole.INSTITUTION_ADMIN)))
                .andExpect(status().isForbidden());
    }

    private Member createInstitutionAdmin(String prefix) {
        return members.save(Member.create(
                prefix + "-institution-" + UUID.randomUUID() + "@example.com",
                "encoded-password", "기관관리자", MemberRole.INSTITUTION_ADMIN));
    }

    private String createGroup(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"DAUGHTER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("groupId").asText();
    }

    private String createElder(String token, String groupId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/elders")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("groupId", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김해미","birthYear":1940,"gender":"FEMALE","residenceType":"HOME_WITH_FAMILY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("elderId").asText();
    }

    private String token(UUID memberId, MemberRole role) {
        return tokenPort.generateAccessToken(memberId, role.name().toLowerCase() + "@example.com", role);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
