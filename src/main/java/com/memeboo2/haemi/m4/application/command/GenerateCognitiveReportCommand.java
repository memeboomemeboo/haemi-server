package com.memeboo2.haemi.m4.application.command;

import com.memeboo2.haemi.m4.domain.model.dashboard.ReportDeliveryMethod;
import com.memeboo2.haemi.m4.domain.model.dashboard.ReportPeriod;

import java.time.LocalDate;

public record GenerateCognitiveReportCommand(
        String elderId,
        String albumId,
        ReportPeriod period,
        LocalDate customFrom,
        LocalDate customTo,
        ReportDeliveryMethod deliveryMethod
) {}
