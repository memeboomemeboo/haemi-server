package com.memeboo2.haemi.m1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import org.junit.jupiter.api.BeforeEach;
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

// F1-02/F1-03/F1-06 보강 사항을 보안 필터 체인·실제 DB를 통해 검증하는 API 레벨 회귀 테스트
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AlbumPhotoApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;

    @Autowired
    AlbumPhotoApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
    }

    @BeforeEach
    void setUp() {
        token = tokenPort.generateAccessToken(UUID.randomUUID(), "family-api-test@example.com", MemberRole.FAMILY);
    }

    @Test
    void inviteMember_rejectsInviterWhoIsNotAlbumMember() throws Exception {
        String albumId = createAlbum("owner-1");

        mockMvc.perform(post("/api/v1/albums/{albumId}/members", albumId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviterId":"stranger","inviteeId":"family-2"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void inviteMember_pendingUntilAccepted_thenBecomesAlbumMember() throws Exception {
        String albumId = createAlbum("owner-2");

        mockMvc.perform(post("/api/v1/albums/{albumId}/members", albumId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviterId":"owner-2","inviteeId":"family-2"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/albums/{albumId}", albumId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberIds", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("family-2"))));

        mockMvc.perform(post("/api/v1/albums/{albumId}/members/{memberId}/accept", albumId, "family-2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/albums/{albumId}", albumId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberIds", org.hamcrest.Matchers.hasItem("family-2")));
    }

    @Test
    void syncPhotos_rejectsCellularWhenWifiOnly() throws Exception {
        String albumId = createAlbum("owner-3");
        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "photo-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/albums/{albumId}/photos/sync", albumId)
                        .file(file)
                        .param("uploadedBy", "owner-3")
                        .param("wifiOnly", "true")
                        .param("networkType", "CELLULAR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void syncPhotos_rejectsLowBattery() throws Exception {
        String albumId = createAlbum("owner-4");
        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "photo-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/albums/{albumId}/photos/sync", albumId)
                        .file(file)
                        .param("uploadedBy", "owner-4")
                        .param("wifiOnly", "false")
                        .param("networkType", "WIFI")
                        .param("batteryLevel", "15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void syncPhotos_succeedsAndRecordsQueryableHistory() throws Exception {
        String albumId = createAlbum("owner-5");
        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "photo-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/albums/{albumId}/photos/sync", albumId)
                        .file(file)
                        .param("uploadedBy", "owner-5")
                        .param("wifiOnly", "true")
                        .param("networkType", "WIFI")
                        .param("batteryLevel", "80")
                        .param("backgroundSync", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.saved.length()").value(1));

        mockMvc.perform(get("/api/v1/albums/{albumId}/photos/sync/history", albumId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].savedCount").value(1))
                .andExpect(jsonPath("$.data[0].networkType").value("WIFI"))
                .andExpect(jsonPath("$.data[0].batteryLevel").value(80));
    }

    @Test
    void timeline_belowThreeShotPhotosReturnsGuideMessageAndRoleControlsEditable() throws Exception {
        String albumId = createAlbum("owner-6");
        MockMultipartFile file = new MockMultipartFile("files", "photo.jpg", "image/jpeg", "photo-bytes".getBytes());
        mockMvc.perform(multipart("/api/v1/albums/{albumId}/photos", albumId)
                        .file(file)
                        .param("uploadedBy", "owner-6")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/albums/{albumId}/timeline", albumId)
                        .param("role", "FAMILY")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.belowMinimumPhotoThreshold").value(true))
                .andExpect(jsonPath("$.data.guideMessage").value("사진을 더 추가하면 타임라인이 만들어집니다"))
                .andExpect(jsonPath("$.data.editable").value(true));

        mockMvc.perform(get("/api/v1/albums/{albumId}/timeline", albumId)
                        .param("role", "ELDER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.editable").value(false));
    }

    private String createAlbum(String ownerMemberId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/albums")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderProfileId":"elder-api-test","groupId":"group-%s","ownerMemberId":"%s"}
                                """.formatted(UUID.randomUUID(), ownerMemberId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String albumId = root.path("data").path("albumId").asText();
        assertThat(albumId).isNotBlank();
        return albumId;
    }
}
