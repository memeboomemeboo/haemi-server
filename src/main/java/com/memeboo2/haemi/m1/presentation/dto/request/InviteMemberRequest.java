package com.memeboo2.haemi.m1.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "멤버 초대 요청")
public record InviteMemberRequest(
        @Schema(description = "초대받는 memberId(UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull UUID inviteeId
) {}
