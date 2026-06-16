package com.memeboo2.haemi.m4.application.query;

import java.time.LocalDate;

public record GetCognitiveMetricQuery(
        String elderId,
        LocalDate from,
        LocalDate to
) {}
