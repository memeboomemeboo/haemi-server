package com.memeboo2.haemi.m4.application.query;

import java.time.LocalDate;
import java.util.List;

public record GetInstitutionDashboardQuery(
        String institutionId,
        LocalDate from,
        LocalDate to,
        List<String> elderIds
) {}
