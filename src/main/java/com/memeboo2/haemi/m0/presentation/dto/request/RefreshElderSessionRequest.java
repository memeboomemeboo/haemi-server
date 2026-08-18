package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshElderSessionRequest(
        @NotBlank String refreshToken,
        @NotBlank @Size(max = 128) String deviceId
) {
}
