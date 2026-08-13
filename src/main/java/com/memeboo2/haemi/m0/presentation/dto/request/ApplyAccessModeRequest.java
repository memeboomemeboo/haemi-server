package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.access.EntryPath;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "접근 모드 추천 적용 요청. 대행 실행자는 인증된 요청자로 자동 기록됩니다.")
public record ApplyAccessModeRequest(
        @NotNull EntryPath entryPath
) {}
