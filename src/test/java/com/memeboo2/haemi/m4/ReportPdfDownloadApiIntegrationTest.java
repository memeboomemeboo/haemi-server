package com.memeboo2.haemi.m4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memeboo2.haemi.auth.domain.model.MemberRole;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportMode;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 리포트 PDF 다운로드 (#93).
 *
 * <p>재배포로 파일이 사라진 리포트가 200으로 시작했다가 본문에서 깨지지 않는지 확인한다.
 */
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportPdfDownloadApiIntegrationTest {

    private final MockMvc mockMvc;
    private final TokenPort tokenPort;
    private final CognitiveReportRepository reports;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ReportPdfDownloadApiIntegrationTest(MockMvc mockMvc, TokenPort tokenPort, CognitiveReportRepository reports) {
        this.mockMvc = mockMvc;
        this.tokenPort = tokenPort;
        this.reports = reports;
    }

    @Test
    @DisplayName("파일이 사라진 리포트는 500이 아니라 재생성 안내와 함께 404를 준다")
    void missingPdfFileReturnsNotFoundInsteadOfBrokenResponse() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String token = familyToken(ownerId);
        String elderId = createElder(token, createGroup(token));
        CognitiveReport report = saveReport(elderId, "/nonexistent/haemi/reports/gone.pdf");

        mockMvc.perform(get("/api/v1/cognitive-dashboard/reports/{reportId}/pdf", report.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("다시 만들어 주세요")));
    }

    @Test
    @DisplayName("파일이 있는 리포트는 그대로 내려받힌다")
    void existingPdfFileIsDownloadable(@TempDir Path tempDir) throws Exception {
        UUID ownerId = UUID.randomUUID();
        String token = familyToken(ownerId);
        String elderId = createElder(token, createGroup(token));
        Path pdf = Files.writeString(tempDir.resolve("report.pdf"), "%PDF-1.4 test");
        CognitiveReport report = saveReport(elderId, pdf.toString());

        mockMvc.perform(get("/api/v1/cognitive-dashboard/reports/{reportId}/pdf", report.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("report.pdf")));
    }

    private CognitiveReport saveReport(String elderId, String pdfKey) {
        CognitiveReport report = CognitiveReport.createReminiscence(
                elderId, UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.now().minusDays(6), LocalDate.now(), ReportMode.MEMORY_FOCUSED,
                4, List.of("고향"), List.of("photo-1"), 2, 1,
                null, "함께한 기억을 정리했어요.", ReportDeliveryMethod.IN_APP);
        report.assignPdfKey(pdfKey);
        return reports.save(report);
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
