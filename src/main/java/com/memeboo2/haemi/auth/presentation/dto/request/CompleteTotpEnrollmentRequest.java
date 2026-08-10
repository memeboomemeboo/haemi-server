package com.memeboo2.haemi.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "기관 관리자 2FA 최초 등록 확정 요청")
public record CompleteTotpEnrollmentRequest(
        @Schema(description = "이메일", example = "admin@haemi.kr")
        @NotBlank @Email String email,

        @Schema(description = "비밀번호")
        @NotBlank String password,

        @Schema(description = "등록 시작에서 받은 비밀키")
        @NotBlank String secret,

        @Schema(description = "인증 앱에서 생성된 6자리 코드", example = "123456")
        @NotBlank @Size(min = 6, max = 6) String code
) {}
