package com.memeboo2.haemi.m3.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestGrandchildChanceRequest(
        @NotBlank(message = "어르신 ID는 필수입니다.")
        String elderId
) {}
