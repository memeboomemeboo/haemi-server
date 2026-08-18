package com.memeboo2.haemi.m0.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "어르신 설정 저장 요청")
public record SaveElderDisplaySettingRequest(
        @Schema(description = "폰트 크기 레벨 (1=기본, 2=크게, 3=더 크게)", example = "2")
        Integer fontSizeLevel,

        @Schema(description = "음성 기능 사용 여부", example = "true")
        Boolean voiceFeatureEnabled,

        @Schema(description = "알림 사용 여부", example = "true")
        Boolean notificationEnabled
) {}
