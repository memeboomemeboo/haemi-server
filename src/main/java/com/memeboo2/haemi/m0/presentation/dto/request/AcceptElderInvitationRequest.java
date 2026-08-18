package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 어르신 화면에서 1회 입력하는 세 항목과 기기 식별자 (F0-01-E). */
public record AcceptElderInvitationRequest(
        @NotBlank @Size(min = 2, max = 20) String name,
        @NotBlank String phoneNumber,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "코드를 다시 확인해주세요.") String code,
        @NotBlank @Size(max = 128) String deviceId
) {
}
