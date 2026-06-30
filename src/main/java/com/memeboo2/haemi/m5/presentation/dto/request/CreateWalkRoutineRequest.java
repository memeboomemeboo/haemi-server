package com.memeboo2.haemi.m5.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public record CreateWalkRoutineRequest(
        @NotBlank(message = "어르신 ID는 필수입니다.")
        String elderId,
        @NotBlank(message = "가족 그룹 ID는 필수입니다.")
        String groupId,
        LocalTime morningTime,
        LocalTime afternoonTime,
        int targetMinutes
) {}
