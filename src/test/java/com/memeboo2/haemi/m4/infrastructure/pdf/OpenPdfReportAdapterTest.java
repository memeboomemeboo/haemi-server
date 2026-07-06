package com.memeboo2.haemi.m4.infrastructure.pdf;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportTrendPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

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
    @DisplayName("주간 리포트 PDF를 생성하고 파일 경로를 반환한다")
    void generatePdf_createsWeeklyReport() {
        CognitiveReport report = CognitiveReport.create(
                "elder-1", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                5, 0.75, 25.0, 3, 4, "FAMILY",
                0.05, -2.0, trend(), "요약", ReportDeliveryMethod.IN_APP);

        String pdfKey = adapter.generatePdf(report);

        assertThat(pdfKey).endsWith(".pdf");
        File file = new File(pdfKey);
        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(0);
    }

    @Test
    @DisplayName("월간 리포트 PDF를 생성한다")
    void generatePdf_createsMonthlyReport() {
        CognitiveReport report = CognitiveReport.create(
                "elder-2", UUID.randomUUID(), ReportPeriod.MONTHLY,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                20, 0.68, 30.0, 10, 7, "FAMILY",
                -0.02, 3.0, trend(), "요약", ReportDeliveryMethod.IN_APP);

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

        CognitiveReport report = CognitiveReport.create(
                "elder-3", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                3, 0.9, 15.0, 1, 2, "PORTRAIT",
                0.1, -1.0, trend(), "요약", ReportDeliveryMethod.IN_APP);

        String pdfKey = nestedAdapter.generatePdf(report);

        File file = new File(pdfKey);
        assertThat(file).exists();
    }

    @Test
    @DisplayName("changeSummary가 null이면 No data로 처리한다")
    void generatePdf_nullSummary() {
        CognitiveReport report = CognitiveReport.create(
                "elder-4", UUID.randomUUID(), ReportPeriod.WEEKLY,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 6),
                1, 0.5, 40.0, 0, 0, null,
                0.0, 0.0, List.of(), null, ReportDeliveryMethod.IN_APP);

        String pdfKey = adapter.generatePdf(report);

        File file = new File(pdfKey);
        assertThat(file).exists();
    }

    private List<ReportTrendPoint> trend() {
        return List.of(
                new ReportTrendPoint(LocalDate.of(2026, 7, 1), 0.6),
                new ReportTrendPoint(LocalDate.of(2026, 7, 2), 0.75),
                new ReportTrendPoint(LocalDate.of(2026, 7, 3), 0.9)
        );
    }
}
