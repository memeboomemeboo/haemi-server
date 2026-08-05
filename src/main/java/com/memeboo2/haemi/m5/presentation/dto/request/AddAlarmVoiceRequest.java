package com.memeboo2.haemi.m5.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "목소리 알람 로테이션 음성 추가 요청 [F5-01]")
public record AddAlarmVoiceRequest(
        @Schema(description = "어르신 ID", example = "elder-001")
        @NotBlank String elderId
) {}
