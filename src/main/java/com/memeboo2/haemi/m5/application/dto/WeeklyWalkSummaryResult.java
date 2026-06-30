package com.memeboo2.haemi.m5.application.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklyWalkSummaryResult(
        String elderId,
        LocalDate from,
        LocalDate to,
        int completedDays,
        double achievementRate,
        List<WalkRecordResult> records
) {}
