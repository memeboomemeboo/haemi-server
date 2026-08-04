package com.memeboo2.haemi.m4.application.command;

import java.time.LocalDate;

/** v3.0 회상 기록 집계 입력. 음성 내용이나 정오답은 저장하지 않는다. */
public record RecordReminiscenceMetricCommand(
        String elderId,
        String albumId,
        String institutionId,
        LocalDate metricDate,
        int sessionCount,
        int voiceDetectedCount,
        double averageDwellMs,
        int hintPlaybackCount,
        int hintNoResponseCount,
        int familyContributionCount,
        String topMemoryTopic,
        String topDwelledPhoto
) {
}
