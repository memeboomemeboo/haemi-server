package com.memeboo2.haemi.m4.infrastructure.export;

import com.memeboo2.haemi.m4.application.dto.InstitutionDashboardResult;
import com.memeboo2.haemi.m4.domain.model.dashboard.DashboardExportFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstitutionDashboardExportAdapterTest {

    private final InstitutionDashboardExportAdapter adapter =
            new InstitutionDashboardExportAdapter();

    @Test
    @DisplayName("CSV 내보내기는 실명 식별자를 제외하고 익명 식별자와 지표만 포함한다")
    void exportCsv_excludesRawElderIdentifier() {
        var result = adapter.export(dashboard(), DashboardExportFormat.CSV);
        String csv = new String(result.content(), StandardCharsets.UTF_8);

        assertThat(result.contentType()).startsWith("text/csv");
        assertThat(csv).contains("senior-ab12", "participationRate");
        assertThat(csv).doesNotContain("elder-sensitive-id");
    }

    @Test
    @DisplayName("PDF 내보내기는 유효한 PDF 바이트를 생성한다")
    void exportPdf_createsPdfDocument() {
        var result = adapter.export(dashboard(), DashboardExportFormat.PDF);

        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(new String(result.content(), 0, 4, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF");
    }

    private InstitutionDashboardResult dashboard() {
        return new InstitutionDashboardResult(
                "institution-1",
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 7, 6),
                0.75,
                20.0,
                List.of(new InstitutionDashboardResult.SeniorSummary(
                        "senior-ab12",
                        "elder-sensitive-id",
                        5,
                        5.0 / 7.0,
                        0.8,
                        18.0,
                        0.1,
                        0.05
                ))
        );
    }
}
