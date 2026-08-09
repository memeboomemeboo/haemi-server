package com.memeboo2.haemi.m1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemoryFeedApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MemoryFeedApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
    }

    @Test
    void familyOnlyMemoryNeverAppearsInElderFeedButGroupAllDoes() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String familyToken = familyToken(ownerId);
        String groupId = createGroup(familyToken);
        String elderId = createElder(familyToken, groupId);
        String elderToken = tokenPort.generateAccessToken(UUID.randomUUID(), "elder-memory@example.com", MemberRole.ELDER);

        createMemory(familyToken, groupId, "가족끼리만 보는 개인 기록이에요.", "FAMILY_ONLY");

        mockMvc.perform(get("/api/v1/groups/{groupId}/memories", groupId)
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memories.length()").value(1));

        // EX-F103-05 S1: 가족 전용 기록은 어르신 조회·사전 전달 후보에서 완전히 제외된다.
        mockMvc.perform(get("/api/v1/elders/{elderId}/memories", elderId)
                        .header("Authorization", "Bearer " + elderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memories.length()").value(0));

        createMemory(familyToken, groupId, "1980년 여름 바닷가에서 찍은 사진이에요.", "GROUP_ALL");

        mockMvc.perform(get("/api/v1/elders/{elderId}/memories", elderId)
                        .header("Authorization", "Bearer " + elderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memories.length()").value(1))
                .andExpect(jsonPath("$.data.memories[0].visibility").value("GROUP_ALL"));
    }

    @Test
    void abusiveTextIsBlockedByServerModeration() throws Exception {
        String familyToken = familyToken(UUID.randomUUID());
        String groupId = createGroup(familyToken);

        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                "{\"textContent\":\"너는 죽어\",\"visibility\":\"GROUP_ALL\"}".getBytes());
        mockMvc.perform(multipart("/api/v1/groups/{groupId}/memories", groupId)
                        .file(data)
                        .header("Authorization", "Bearer " + familyToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이 표현은 담을 수 없어요."));
    }

    private String createGroup(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"relation\":\"DAUGHTER\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String groupId = json(result).path("data").path("groupId").asText();
        assertThat(groupId).isNotBlank();
        return groupId;
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

    private void createMemory(String token, String groupId, String text, String visibility) throws Exception {
        MockMultipartFile data = new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE,
                ("{\"textContent\":\"%s\",\"visibility\":\"%s\"}".formatted(text, visibility)).getBytes());
        mockMvc.perform(multipart("/api/v1/groups/{groupId}/memories", groupId)
                        .file(data)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private String familyToken(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "family-%s@example.com".formatted(memberId), MemberRole.FAMILY);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
