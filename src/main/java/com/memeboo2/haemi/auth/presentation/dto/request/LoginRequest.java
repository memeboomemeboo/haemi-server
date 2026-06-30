package com.memeboo2.haemi.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "이메일", example = "family@haemi.kr")
        @NotBlank @Email
        String email,

        @Schema(description = "비밀번호", example = "Haemi123!")
        @NotBlank
        String password,

        @Schema(description = "2FA TOTP 코드 (기관 관리자용, 6자리)", example = "123456", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String totpCode
) {}
