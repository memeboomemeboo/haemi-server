package com.memeboo2.haemi.m3.presentation.dto.request;

import com.memeboo2.haemi.m3.domain.model.hint.AccrualSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AccrueHintRequest(
        @NotBlank(message = "어르신 ID는 필수입니다.")
        String elderId,
        UUID photoId,
        String personName,
        @NotNull(message = "적립 경로는 필수입니다.")
        AccrualSource source,
        @NotBlank(message = "적립자 이름은 필수입니다.")
        String authorName,
        @NotBlank(message = "힌트 내용은 필수입니다.")
        String text
) {}
