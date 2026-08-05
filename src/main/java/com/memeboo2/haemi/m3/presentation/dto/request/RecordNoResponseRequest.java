package com.memeboo2.haemi.m3.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecordNoResponseRequest(
        @NotBlank(message = "문제 ID는 필수입니다.")
        String questionId
) {}
