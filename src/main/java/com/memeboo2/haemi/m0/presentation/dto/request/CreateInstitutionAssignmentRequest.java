package com.memeboo2.haemi.m0.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateInstitutionAssignmentRequest(
        @NotBlank(message = "기관 ID는 필수예요.")
        @Size(max = 100, message = "기관 ID는 100자를 초과할 수 없어요.")
        String institutionId,
        @NotNull(message = "기관 관리자 계정은 필수예요.")
        UUID institutionAdminMemberId
) {
}
