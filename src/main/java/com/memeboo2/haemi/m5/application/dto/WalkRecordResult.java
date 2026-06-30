package com.memeboo2.haemi.m5.application.dto;

import com.memeboo2.haemi.m5.domain.model.care.WalkRecord;
import com.memeboo2.haemi.m5.domain.model.care.WalkStatus;
import com.memeboo2.haemi.m5.domain.model.care.WeatherCondition;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WalkRecordResult(
        String walkRecordId,
        String routineId,
        String elderId,
        String groupId,
        LocalDate walkDate,
        WalkStatus status,
        int durationMinutes,
        int stepCount,
        WeatherCondition weatherCondition,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
    public static WalkRecordResult from(WalkRecord record) {
        return new WalkRecordResult(
                record.getId().toString(),
                record.getRoutineId().toString(),
                record.getElderId(),
                record.getGroupId(),
                record.getWalkDate(),
                record.getStatus(),
                record.getDurationMinutes(),
                record.getStepCount(),
                record.getWeatherCondition(),
                record.getStartedAt(),
                record.getCompletedAt()
        );
    }
}
