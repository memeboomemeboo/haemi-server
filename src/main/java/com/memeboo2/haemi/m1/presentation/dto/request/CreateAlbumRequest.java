package com.memeboo2.haemi.m1.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "앨범 생성 요청")
public record CreateAlbumRequest(
        @Schema(description = "어르신 프로필 ID", example = "elder-profile-001")
        @NotBlank String elderProfileId,

        @Schema(description = "가족 그룹 ID", example = "family-group-001")
        @NotBlank String groupId
) {}
