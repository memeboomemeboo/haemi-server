package com.memeboo2.haemi.m3.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerTrainingQuestionRequest(
        @NotBlank(message = "문제 ID는 필수입니다.")
        String questionId,
        @NotNull(message = "발화 감지 여부는 필수입니다.")
        Boolean voiceDetected,
        @Min(value = 0, message = "발화 길이는 0ms 이상이어야 합니다.")
        int vadDurationMs
) {}
