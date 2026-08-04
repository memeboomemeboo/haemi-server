package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.access.EntryPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "접근 모드 추천 적용 요청. 대행 실행이면 entryPath=CAREGIVER + operatorId 필수")
public record ApplyAccessModeRequest(
        @NotNull EntryPath entryPath,
        @Schema(description = "대행 실행자 ID (CAREGIVER일 때 필수)")
        UUID operatorId
) {}
