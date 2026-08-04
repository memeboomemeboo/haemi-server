package com.memeboo2.haemi.m4.infrastructure.pdf;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportMode;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfReportAdapterTest {

    @TempDir
    Path tempDir;

    OpenPdfReportAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OpenPdfReportAdapter(tempDir, "");
    }

    @Test
    @DisplayName("주간 회상 리포트 PDF는 평가 그래프 없이 회상 기록만 생성한다")
    void generatePdf_createsWeeklyReport() {
        CognitiveReport report = CognitiveReport.createReminiscence(
                "elder-1", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                ReportMode.STANDARD, 5, java.util.List.of("family", "seaside"), java.util.List.of("photo-1"),
                3, 4, "Days together grew by one.", "Family stories were shared.", ReportDeliveryMethod.IN_APP);

        String pdfKey = adapter.generatePdf(report);

        assertThat(pdfKey).endsWith(".pdf");
        File file = new File(pdfKey);
        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0);
        assertThat(extractText(pdfKey)).contains("Reminiscence", "Memories Shared", "not a medical diagnosis")
                .doesNotContain("Accuracy", "Trend", "Response Time", "Cognitive Training");
    }

    @Test
    @DisplayName("월간 리포트 PDF를 생성한다")
    void generatePdf_createsMonthlyReport() {
        CognitiveReport report = CognitiveReport.createReminiscence(
                "elder-2", UUID.randomUUID(), ReportPeriod.MONTHLY,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                ReportMode.MEMORY_FOCUSED, 20, java.util.List.of("home"), java.util.List.of("photo-2"),
                7, 10, null, "Memories were collected.", ReportDeliveryMethod.IN_APP);

        String pdfKey = adapter.generatePdf(report);

        File file = new File(pdfKey);
        assertThat(file).exists();
        assertThat(file.getName()).contains("elder-2");
        assertThat(file.getName()).contains("MONTHLY");
    }

    @Test
    @DisplayName("출력 디렉토리가 없으면 자동 생성한다")
    void generatePdf_createsOutputDirectory() {
        Path newDir = tempDir.resolve("nested").resolve("new");
        OpenPdfReportAdapter nestedAdapter = new OpenPdfReportAdapter(newDir, "");

        CognitiveReport report = CognitiveReport.createReminiscence(
                "elder-3", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                ReportMode.STANDARD, 3, java.util.List.of("portrait"), java.util.List.of(),
                2, 1, "Days together stayed the same.", "Memories were collected.", ReportDeliveryMethod.IN_APP);

        String pdfKey = nestedAdapter.generatePdf(report);

        File file = new File(pdfKey);
        assertThat(file).exists();
    }

    @Test
    @DisplayName("주제와 음성 기록이 없어도 빈 그래프 없이 PDF를 생성한다")
    void generatePdf_nullSummary() {
        CognitiveReport report = CognitiveReport.createReminiscence(
                "elder-4", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                ReportMode.MEMORY_FOCUSED, 1, java.util.List.of(), java.util.List.of(),
                0, 0, null, null, ReportDeliveryMethod.IN_APP);

        String pdfKey = adapter.generatePdf(report);

        File file = new File(pdfKey);
        assertThat(file).exists();
    }

    private String extractText(String pdfKey) {
        try {
            PdfReader reader = new PdfReader(pdfKey);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page));
            }
            reader.close();
            return text.toString().replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            throw new AssertionError("PDF text extraction failed", e);
        }
    }
}
