package com.memeboo2.haemi.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "기관 관리자 2FA 최초 등록 시작 요청")
public record BeginTotpEnrollmentRequest(
        @Schema(description = "이메일", example = "admin@haemi.kr")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호")
        @NotBlank String password
) {}
