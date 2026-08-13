package com.memeboo2.haemi.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceTokenHeartbeatRequest(
        @NotBlank(message = "기기 토큰은 필수입니다.") String token
) {}
