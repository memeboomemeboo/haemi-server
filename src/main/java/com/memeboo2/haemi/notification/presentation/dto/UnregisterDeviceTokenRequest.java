package com.memeboo2.haemi.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "기기 알림 토큰 해지 요청")
public record UnregisterDeviceTokenRequest(
        @Schema(description = "해지할 FCM 등록 토큰")
        @NotBlank @Size(max = 255) String token
) {}
