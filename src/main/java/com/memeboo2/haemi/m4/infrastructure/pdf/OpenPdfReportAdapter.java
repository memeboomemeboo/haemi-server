package com.memeboo2.haemi.m4.infrastructure.pdf;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportMode;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Component
public class OpenPdfReportAdapter implements PdfReportPort {

    private final Path outputDir;
    private final String fontPath;

    public OpenPdfReportAdapter(
            @Value("${haemi.report.pdf.output-dir:reports}") Path outputDir,
            @Value("${haemi.report.pdf.font-path:}") String fontPath) {
        this.outputDir = Paths.get(outputDir.toString());
        this.fontPath = fontPath;
    }

    @Override
    public String generatePdf(CognitiveReport report) {
        try {
            java.nio.file.Files.createDirectories(outputDir);
            String fileName = "%s-%s-%s.pdf".formatted(
                    report.getElderId(),
                    report.getPeriod(),
                    report.getPeriodEnd().format(DateTimeFormatter.ISO_LOCAL_DATE));
            Path outputPath = outputDir.resolve(fileName);
            String pdfKey = outputPath.toString();

            BaseFont baseFont;
            if (fontPath != null && !fontPath.isBlank()) {
                baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            }
            Font titleFont = new Font(baseFont, 16, Font.BOLD);
            Font sectionFont = new Font(baseFont, 12, Font.BOLD);
            Font bodyFont = new Font(baseFont, 10, Font.NORMAL);
            Font smallItalicFont = new Font(baseFont, 8, Font.ITALIC);

            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfKey));
            document.open();

            String periodLabel = report.getPeriod().name().equals("WEEKLY") ? "Weekly" : "Monthly";
            document.add(new Paragraph("Reminiscence %s Record".formatted(periodLabel), titleFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            document.add(new Paragraph("Period: %s ~ %s".formatted(
                    report.getPeriodStart().format(dtf),
                    report.getPeriodEnd().format(dtf)), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("Memories Shared", sectionFont));
            document.add(new Paragraph("Days together: %d".formatted(report.getDaysTogether()), bodyFont));
            document.add(new Paragraph("Remembered topics: %s".formatted(
                    report.getRememberedTopics() != null ? report.getRememberedTopics().replace("|", ", ") : "No topic recorded"), bodyFont));
            document.add(new Paragraph("Photos stayed with: %s".formatted(
                    report.getTopDwelledPhotos() != null ? report.getTopDwelledPhotos().replace("|", ", ") : "No photo recorded"), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            if (report.getVoiceResponseCount() > 0) {
                document.add(new Paragraph("Voice Moments", sectionFont));
                document.add(new Paragraph("Voice responses recorded: %d".formatted(
                        report.getVoiceResponseCount()), bodyFont));
                document.add(new Paragraph(Chunk.NEWLINE));
            }

            document.add(new Paragraph("Family Memories", sectionFont));
            document.add(new Paragraph("Family records added: %d".formatted(
                    report.getFamilyContributionCount()), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            if (report.getReportMode() == ReportMode.STANDARD && report.getActivityMessage() != null) {
                document.add(new Paragraph("Activity Note", sectionFont));
                document.add(new Paragraph(report.getActivityMessage(), bodyFont));
                document.add(new Paragraph(Chunk.NEWLINE));
            }

            document.add(new Paragraph("(This record is not a medical diagnosis.)", smallItalicFont));

            document.close();
            return pdfKey;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

}
