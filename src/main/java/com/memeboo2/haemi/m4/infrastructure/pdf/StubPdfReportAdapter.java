package com.memeboo2.haemi.m4.infrastructure.pdf;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.port.PdfReportPort;
import org.springframework.stereotype.Component;

@Component
public class StubPdfReportAdapter implements PdfReportPort {

    @Override
    public String generatePdf(CognitiveReport report) {
        return "reports/%s-%s-%s.pdf".formatted(
                report.getElderId(), report.getPeriod(), report.getPeriodEnd());
    }
}
