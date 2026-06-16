package com.memeboo2.haemi.m4.domain.port;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;

public interface PdfReportPort {
    String generatePdf(CognitiveReport report);
}
