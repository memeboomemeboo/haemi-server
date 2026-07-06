package com.memeboo2.haemi.m4.infrastructure.pdf;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Color;
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

            String periodLabel = report.getPeriod() == ReportPeriod.WEEKLY ? "Weekly" : "Monthly";
            document.add(new Paragraph("Cognitive Training %s Report".formatted(periodLabel), titleFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            document.add(new Paragraph("Period: %s ~ %s".formatted(
                    report.getPeriodStart().format(dtf),
                    report.getPeriodEnd().format(dtf)), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("Training Summary", sectionFont));
            document.add(new Paragraph("Session Count: %d".formatted(report.getParticipationCount()), bodyFont));
            document.add(new Paragraph("Average Accuracy: %.1f%%".formatted(report.getAverageAccuracyRate() * 100), bodyFont));
            document.add(new Paragraph("Average Response Time: %.1fs".formatted(report.getAverageResponseSeconds()), bodyFont));
            document.add(new Paragraph("Accuracy Change: %+.1f percentage points".formatted(
                    report.getAccuracyChangeFromPrevious() * 100), bodyFont));
            document.add(new Paragraph("Response Time Change: %+.1fs".formatted(
                    report.getResponseTimeChangeFromPrevious()), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("Accuracy Trend", sectionFont));
            drawAccuracyTrend(writer, document, report, baseFont);
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("Family Participation", sectionFont));
            document.add(new Paragraph("Reminiscence Reactions: %d".formatted(
                    report.getReminiscenceParticipationCount()), bodyFont));
            document.add(new Paragraph("Memory Posts: %d".formatted(report.getMemoryPostCount()), bodyFont));
            document.add(new Paragraph("Most Reacted Photo Type: %s".formatted(
                    report.getMostReactedPhotoType() != null
                            ? report.getMostReactedPhotoType()
                            : "No data"), bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("Change Summary", sectionFont));
            document.add(new Paragraph(
                    "Compared with the previous period: accuracy %+.1f percentage points, response time %+.1fs."
                            .formatted(
                                    report.getAccuracyChangeFromPrevious() * 100,
                                    report.getResponseTimeChangeFromPrevious()),
                    bodyFont));
            document.add(new Paragraph(Chunk.NEWLINE));

            document.add(new Paragraph("(This report is not a medical diagnosis.)", smallItalicFont));

            document.close();
            return pdfKey;
        } catch (IOException | DocumentException e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private void drawAccuracyTrend(PdfWriter writer, Document document, CognitiveReport report,
                                   BaseFont baseFont) {
        if (report.getAccuracyTrend().isEmpty()) {
            return;
        }
        PdfContentByte canvas = writer.getDirectContent();
        float left = document.left();
        float bottom = writer.getVerticalPosition(true) - 105;
        float width = document.right() - document.left();
        float height = 90;

        canvas.saveState();
        canvas.setColorStroke(new Color(110, 118, 129));
        canvas.setLineWidth(0.8f);
        canvas.moveTo(left, bottom);
        canvas.lineTo(left, bottom + height);
        canvas.moveTo(left, bottom);
        canvas.lineTo(left + width, bottom);
        canvas.stroke();

        float slotWidth = width / report.getAccuracyTrend().size();
        float barWidth = Math.max(4, slotWidth * 0.55f);
        for (int index = 0; index < report.getAccuracyTrend().size(); index++) {
            double accuracy = report.getAccuracyTrend().get(index).getAccuracyRate();
            float barHeight = (float) (height * Math.max(0.0, Math.min(1.0, accuracy)));
            float x = left + (index * slotWidth) + ((slotWidth - barWidth) / 2);
            canvas.setColorFill(new Color(47, 111, 173));
            canvas.rectangle(x, bottom, barWidth, barHeight);
            canvas.fill();

            String dateLabel = report.getAccuracyTrend().get(index).getDate()
                    .format(DateTimeFormatter.ofPattern("MM/dd"));
            canvas.beginText();
            canvas.setFontAndSize(baseFont, 7);
            canvas.setColorFill(new Color(70, 70, 70));
            canvas.showTextAligned(
                    PdfContentByte.ALIGN_CENTER,
                    dateLabel,
                    x + (barWidth / 2),
                    bottom - 10,
                    0
            );
            canvas.endText();
        }
        canvas.restoreState();
    }
}
