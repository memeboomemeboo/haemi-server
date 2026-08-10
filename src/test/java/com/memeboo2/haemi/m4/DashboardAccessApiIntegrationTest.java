package com.memeboo2.haemi.m4;

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

import java.time.LocalDate;
import java.util.UUID;

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
class DashboardAccessApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    DashboardAccessApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
    }

    @Test
    void familyMemberCannotReadAnotherGroupsReminiscenceMetrics() throws Exception {
        String ownerToken = familyToken(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        String elderId = createElder(ownerToken, groupId);

        mockMvc.perform(get("/api/v1/cognitive-dashboard/metrics")
                        .param("elderId", elderId)
                        .param("from", LocalDate.now().minusDays(6).toString())
                        .param("to", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + familyToken(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
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

    private String familyToken(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "family-%s@example.com".formatted(memberId), MemberRole.FAMILY);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
