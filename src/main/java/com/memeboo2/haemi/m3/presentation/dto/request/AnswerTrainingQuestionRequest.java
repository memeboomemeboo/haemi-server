package com.memeboo2.haemi.m3.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AnswerTrainingQuestionRequest(
        @NotBlank(message = "문제 ID는 필수입니다.")
        String questionId,
        @NotBlank(message = "답변은 필수입니다.")
        String submittedAnswer,
        @Min(value = 0, message = "반응 시간은 0초 이상이어야 합니다.")
        int responseSeconds
) {}
