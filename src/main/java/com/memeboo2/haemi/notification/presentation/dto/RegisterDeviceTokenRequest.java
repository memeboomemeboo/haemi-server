package com.memeboo2.haemi.notification.presentation.dto;

import com.memeboo2.haemi.notification.domain.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "기기 알림 토큰 등록 요청")
public record RegisterDeviceTokenRequest(
        @Schema(description = "FCM 등록 토큰")
        @NotBlank @Size(max = 255) String token,
        @NotNull DevicePlatform platform
) {}
