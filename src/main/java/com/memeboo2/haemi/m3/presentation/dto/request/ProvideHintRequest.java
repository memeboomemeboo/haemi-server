package com.memeboo2.haemi.m3.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ProvideHintRequest(
        @NotBlank(message = "응답자 ID는 필수입니다.")
        String responderMemberId,
        @NotBlank(message = "응답자 이름은 필수입니다.")
        String responderName,
        @NotBlank(message = "힌트 내용은 필수입니다.")
        String hintText
) {}
