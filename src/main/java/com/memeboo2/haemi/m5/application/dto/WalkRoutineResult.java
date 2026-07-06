package com.memeboo2.haemi.m5.application.dto;

import com.memeboo2.haemi.m5.domain.model.care.WalkRoutine;

import java.time.LocalTime;
import java.time.LocalDateTime;

public record WalkRoutineResult(
        String routineId,
        String elderId,
        String groupId,
        LocalTime morningTime,
        LocalTime afternoonTime,
        int targetMinutes,
        boolean active,
        LocalDateTime lastRemindedAt
) {
    public static WalkRoutineResult from(WalkRoutine routine) {
        return new WalkRoutineResult(
                routine.getId().toString(),
                routine.getElderId(),
                routine.getGroupId(),
                routine.getMorningTime(),
                routine.getAfternoonTime(),
                routine.getTargetMinutes(),
                routine.isActive(),
                routine.getLastRemindedAt()
        );
    }
}
