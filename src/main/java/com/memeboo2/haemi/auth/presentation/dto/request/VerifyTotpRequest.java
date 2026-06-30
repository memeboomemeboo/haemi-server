package com.memeboo2.haemi.auth.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "2FA TOTP 검증 요청")
public record VerifyTotpRequest(
        @Schema(description = "설정 시 받은 비밀키")
        @NotBlank
        String secret,

        @Schema(description = "인증 앱에서 생성된 6자리 코드", example = "123456")
        @NotBlank @Size(min = 6, max = 6)
        String code
) {}
