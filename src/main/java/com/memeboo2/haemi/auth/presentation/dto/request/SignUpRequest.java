package com.memeboo2.haemi.auth.presentation.dto.request;

import com.memeboo2.haemi.auth.domain.model.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record SignUpRequest(
        @Schema(description = "이메일", example = "family@haemi.kr")
        @NotBlank @Email
        String email,

        @Schema(description = "비밀번호 (8~30자, 영문·숫자·특수문자 포함)", example = "Haemi123!")
        @NotBlank @Size(min = 8, max = 30)
        String password,

        @Schema(description = "이름", example = "김해미")
        @NotBlank @Size(max = 50)
        String name,

        @Schema(description = "사용자 유형", example = "FAMILY")
        @NotNull
        MemberRole role
) {}
