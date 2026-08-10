package com.memeboo2.haemi.m0;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.model.Member;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.m0.domain.model.ElderHealth;
import com.memeboo2.haemi.m0.domain.model.PersonContentTense;
import com.memeboo2.haemi.m0.domain.port.PersonExposurePort;
import com.memeboo2.haemi.m0.infrastructure.persistence.JpaElderHealthRepository;
import com.memeboo2.haemi.m0.infrastructure.security.ElderHealthCrypto;
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
class M0ApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final PersonExposurePort personExposures;
    private final JpaElderHealthRepository elderHealth;
    private final ElderHealthCrypto healthCrypto;
    private final MemberRepository members;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    M0ApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, PersonExposurePort personExposures,
                         JpaElderHealthRepository elderHealth, ElderHealthCrypto healthCrypto,
                         MemberRepository members) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.personExposures = personExposures;
        this.elderHealth = elderHealth;
        this.healthCrypto = healthCrypto;
        this.members = members;
    }

    @Test
    void familyGroupInvitationAndOwnershipTransferRequireCorrectActors() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID invitedId = UUID.randomUUID();
        String ownerToken = token(ownerId);
        String invitedToken = token(invitedId);
        String groupId = createGroup(ownerToken);

        MvcResult invitation = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"010-1234-5678\",\"relation\":\"SON\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String invitationToken = json(invitation).path("data").path("token").asText();

        mockMvc.perform(post("/api/v1/invitations/{token}/accept", invitationToken)
                        .header("Authorization", "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(2));

        MvcResult transfer = mockMvc.perform(post("/api/v1/groups/{groupId}/transfer", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientMemberId\":\"%s\"}".formatted(invitedId)))
                .andExpect(status().isCreated())
                .andReturn();
        String transferId = json(transfer).path("data").path("transferId").asText();

        mockMvc.perform(post("/api/v1/groups/{groupId}/transfer/{transferId}/accept", groupId, transferId)
                        .header("Authorization", "Bearer " + invitedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerMemberId").value(invitedId.toString()));

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/{memberId}", groupId, ownerId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void elderHealthIsEncryptedAndProfileDataIsProtectedByGroupMembership() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String ownerToken = token(ownerId);
        String groupId = createGroup(ownerToken);
        String elderId = createElder(ownerToken, groupId);

        mockMvc.perform(patch("/api/v1/elders/{elderId}", elderId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"diagnosisLevel":"MILD","healthConsentId":"consent-1","diagnosedAt":"2026-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasHealthInformation").value(true));

        ElderHealth stored = elderHealth.findById(UUID.fromString(elderId)).orElseThrow();
        assertThat(stored.getDiagnosisEncrypted()).doesNotContain("MILD");
        assertThat(healthCrypto.decrypt(stored.getDiagnosisEncrypted())).isEqualTo("MILD");

        mockMvc.perform(put("/api/v1/elders/{elderId}/life-story", elderId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"category":"HOMETOWN","value":"전주","source":"FAMILY"},
                                  {"category":"MUSIC","value":"트로트","source":"FAMILY"}
                                ]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/elders/{elderId}/sensitive-topics", elderId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"사별한 배우자\",\"reason\":\"언급하지 않기\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/elders/{elderId}/completeness", elderId)
                        .header("Authorization", "Bearer " + token(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void elderProfileCanOnlyLinkAnActiveElderAccount() throws Exception {
        String ownerToken = token(UUID.randomUUID());
        String groupId = createGroup(ownerToken);
        UUID familyMemberId = members.save(Member.create("family-link-%s@example.com".formatted(UUID.randomUUID()),
                "encoded-password", "가족", MemberRole.FAMILY)).getId();

        MvcResult elder = mockMvc.perform(post("/api/v1/elders")
                        .header("Authorization", "Bearer " + ownerToken)
                        .queryParam("groupId", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"김해미","birthYear":1940,"gender":"FEMALE","residenceType":"HOME_WITH_FAMILY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String elderId = json(elder).path("data").path("elderId").asText();

        mockMvc.perform(put("/api/v1/elders/{elderId}/member", elderId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"%s\"}".formatted(familyMemberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void hiddenPersonIsImmediatelyExcludedFromPhotoContentContext() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String ownerToken = token(ownerId);
        String groupId = createGroup(ownerToken);
        String personId = createDeceasedPerson(ownerToken, groupId);
        String photoId = createPhoto(ownerToken, groupId);

        mockMvc.perform(post("/api/v1/photos/{photoId}/persons", photoId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":\"%s\",\"confidence\":0.2,\"confirmed\":true}".formatted(personId)))
                .andExpect(status().isCreated());

        PersonExposurePort.PhotoPersonExposure before = personExposures.findByPhotoId(UUID.fromString(photoId)).getFirst();
        assertThat(before.tense()).isEqualTo(PersonContentTense.PAST_ONLY);
        assertThat(before.nameUsable()).isTrue();

        mockMvc.perform(patch("/api/v1/persons/{personId}", personId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visibility\":\"HIDDEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentTense").value("EXCLUDED"));

        PersonExposurePort.PhotoPersonExposure after = personExposures.findByPhotoId(UUID.fromString(photoId)).getFirst();
        assertThat(after.tense()).isEqualTo(PersonContentTense.EXCLUDED);

        mockMvc.perform(get("/api/v1/groups/{groupId}/persons", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("visibility", "SHOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void ownerWithdrawalAutomaticallyTransfersToOldestFamilyMember() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID successorId = UUID.randomUUID();
        String ownerEmail = "withdraw-owner-%s@example.com".formatted(ownerId);
        members.save(Member.create(ownerEmail, "encoded-password", "대표보호자", MemberRole.FAMILY));
        // The persisted aggregate supplies its own ID, so issue the JWT for that identity.
        UUID persistedOwnerId = members.findByEmail(ownerEmail).orElseThrow().getId();
        String ownerToken = token(persistedOwnerId);
        String successorToken = token(successorId);
        String groupId = createGroup(ownerToken);

        MvcResult invitation = mockMvc.perform(post("/api/v1/groups/{groupId}/invitations", groupId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"010-9876-5432\",\"relation\":\"SON\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        mockMvc.perform(post("/api/v1/invitations/{token}/accept", json(invitation).path("data").path("token").asText())
                        .header("Authorization", "Bearer " + successorToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                        .header("Authorization", "Bearer " + successorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerMemberId").value(successorId.toString()));
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

    private String createDeceasedPerson(String token, String groupId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/{groupId}/persons", groupId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"박영수","relation":"SPOUSE","lifeStatus":"DECEASED","deceasedAt":"2020-01-01"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("data").path("personId").asText();
    }

    private String createPhoto(String token, String groupId) throws Exception {
        String elderId = createElder(token, groupId);
        MvcResult album = mockMvc.perform(post("/api/v1/albums")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"elderProfileId":"%s","groupId":"%s"}
                                """.formatted(elderId, groupId)))
                .andExpect(status().isCreated())
                .andReturn();
        String albumId = json(album).path("data").path("albumId").asText();
        MockMultipartFile file = new MockMultipartFile("files", "memory.jpg", "image/jpeg", "photo".getBytes());
        MvcResult photo = mockMvc.perform(multipart("/api/v1/albums/{albumId}/photos", albumId)
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        return json(photo).path("data").get(0).path("photoId").asText();
    }

    private String token(UUID memberId) {
        return tokenPort.generateAccessToken(memberId, "m0-%s@example.com".formatted(memberId), MemberRole.FAMILY);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
