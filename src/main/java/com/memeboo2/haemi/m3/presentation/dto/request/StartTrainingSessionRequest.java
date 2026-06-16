package com.memeboo2.haemi.m3.presentation.dto.request;

import com.memeboo2.haemi.m3.domain.model.training.StartMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartTrainingSessionRequest(
        @NotBlank(message = "어르신 ID는 필수입니다.")
        String elderId,
        @NotBlank(message = "앨범 ID는 필수입니다.")
        String albumId,
        @NotNull(message = "시작 방식은 필수입니다.")
        StartMode startMode
) {}
