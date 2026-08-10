package com.memeboo2.haemi.m0.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Mode A 어르신 계정 연결 요청")
public record LinkElderMemberRequest(
        @NotNull
        @Schema(description = "ELDER 역할의 회원 ID") UUID memberId
) {}
