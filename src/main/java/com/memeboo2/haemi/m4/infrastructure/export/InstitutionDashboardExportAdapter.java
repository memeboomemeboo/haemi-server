package com.memeboo2.haemi.m4.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.memeboo2.haemi.m4.application.dto.InstitutionDashboardExportResult;
import com.memeboo2.haemi.m4.application.dto.InstitutionDashboardResult;
import com.memeboo2.haemi.m4.domain.model.dashboard.DashboardExportFormat;
import com.memeboo2.haemi.m4.domain.port.InstitutionDashboardExportPort;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class InstitutionDashboardExportAdapter implements InstitutionDashboardExportPort {

    @Override
    public InstitutionDashboardExportResult export(
            InstitutionDashboardResult dashboard,
            DashboardExportFormat format
    ) {
        return switch (format) {
            case CSV -> exportCsv(dashboard);
            case PDF -> exportPdf(dashboard);
        };
    }

    private InstitutionDashboardExportResult exportCsv(
            InstitutionDashboardResult dashboard
    ) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("anonymizedSeniorId,participationRate,averageAccuracyRate,")
                .append("averageResponseSeconds,weeklyAccuracyChange,")
                .append("accuracyDeltaFromInstitution\n");
        dashboard.seniors().forEach(senior -> csv.append(senior.anonymizedSeniorId()).append(',')
                .append(formatDecimal(senior.participationRate())).append(',')
                .append(formatDecimal(senior.averageAccuracyRate())).append(',')
                .append(formatDecimal(senior.averageResponseSeconds())).append(',')
                .append(formatDecimal(senior.weeklyAccuracyChange())).append(',')
                .append(formatDecimal(senior.accuracyDeltaFromInstitution())).append('\n'));
        return new InstitutionDashboardExportResult(
                csv.toString().getBytes(StandardCharsets.UTF_8),
                "text/csv;charset=UTF-8",
                fileName(dashboard, "csv")
        );
    }

    private InstitutionDashboardExportResult exportPdf(
            InstitutionDashboardResult dashboard
    ) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            BaseFont baseFont = BaseFont.createFont(
                    BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            Font title = new Font(baseFont, 16, Font.BOLD);
            Font body = new Font(baseFont, 9, Font.NORMAL);
            document.add(new Paragraph("Institution Cognitive Dashboard", title));
            document.add(new Paragraph(
                    "Period: %s ~ %s".formatted(dashboard.from(), dashboard.to()), body));
            document.add(new Paragraph(
                    "Institution average accuracy: %.1f%%".formatted(
                            dashboard.institutionAverageAccuracyRate() * 100), body));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addCell(table, "Senior", body);
            addCell(table, "Participation", body);
            addCell(table, "Accuracy", body);
            addCell(table, "Response", body);
            addCell(table, "Weekly change", body);
            dashboard.seniors().forEach(senior -> {
                addCell(table, senior.anonymizedSeniorId(), body);
                addCell(table, "%.1f%%".formatted(senior.participationRate() * 100), body);
                addCell(table, "%.1f%%".formatted(senior.averageAccuracyRate() * 100), body);
                addCell(table, "%.1fs".formatted(senior.averageResponseSeconds()), body);
                addCell(table, "%+.1f%%p".formatted(senior.weeklyAccuracyChange() * 100), body);
            });
            document.add(table);
            document.close();
            return new InstitutionDashboardExportResult(
                    output.toByteArray(),
                    "application/pdf",
                    fileName(dashboard, "pdf")
            );
        } catch (Exception e) {
            throw new IllegalStateException("기관 대시보드 내보내기에 실패했습니다.", e);
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        table.addCell(new Paragraph(text, font));
    }

    private String fileName(InstitutionDashboardResult dashboard, String extension) {
        return "institution-%s-%s-%s.%s".formatted(
                dashboard.institutionId(), dashboard.from(), dashboard.to(), extension);
    }

    private String formatDecimal(double value) {
        return "%.4f".formatted(value);
    }
}
