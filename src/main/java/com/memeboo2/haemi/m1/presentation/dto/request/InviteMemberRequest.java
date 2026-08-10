package com.memeboo2.haemi.m1.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "멤버 초대 요청")
public record InviteMemberRequest(
        @Schema(description = "초대받는 memberId", example = "member-003")
        @NotBlank String inviteeId
) {}
