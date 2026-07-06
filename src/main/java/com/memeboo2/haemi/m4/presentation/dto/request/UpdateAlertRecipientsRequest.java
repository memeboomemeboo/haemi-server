package com.memeboo2.haemi.m4.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record UpdateAlertRecipientsRequest(
        @NotBlank(message = "주 보호자 회원 ID는 필수입니다.")
        String primaryCaregiverMemberId,
        Set<String> institutionManagerMemberIds
) {}
