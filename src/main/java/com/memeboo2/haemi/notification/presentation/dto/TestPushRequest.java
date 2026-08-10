package com.memeboo2.haemi.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "테스트 알림 발송 요청 (dev 전용)")
public record TestPushRequest(
        @NotBlank String title,
        @NotBlank String body,
        @Schema(description = "단말 라우팅용 부가 데이터 (선택)")
        Map<String, String> data
) {}
