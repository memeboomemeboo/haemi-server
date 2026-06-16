package com.memeboo2.haemi.m5.presentation.dto.request;

import jakarta.validation.constraints.Min;

public record CompleteWalkRequest(
        @Min(value = 0, message = "산책 시간은 0분 이상이어야 합니다.")
        int durationMinutes,
        @Min(value = 0, message = "걸음 수는 0 이상이어야 합니다.")
        int stepCount
) {}
