package com.memeboo2.haemi.m5.application.command;

import java.time.LocalTime;

public record CreateWalkRoutineCommand(
        String elderId,
        String groupId,
        LocalTime morningTime,
        LocalTime afternoonTime,
        int targetMinutes
) {}
