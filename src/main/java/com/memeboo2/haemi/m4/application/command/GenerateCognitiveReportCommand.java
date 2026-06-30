package com.memeboo2.haemi.m4.application.command;

import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;

public record GenerateCognitiveReportCommand(
        String elderId,
        String albumId,
        ReportPeriod period,
        ReportDeliveryMethod deliveryMethod
) {}
