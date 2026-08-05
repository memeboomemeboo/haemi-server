package com.memeboo2.haemi.eventlog.application;

import com.memeboo2.haemi.eventlog.domain.EventType;

import java.time.LocalDate;
import java.util.Map;

public record DailyEventSummary(
        LocalDate date,
        Map<EventType, Long> countsByType,
        long total
) {}
