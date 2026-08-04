package com.memeboo2.haemi.m0.presentation.dto.request;

import com.memeboo2.haemi.m0.domain.model.ElderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "어르신 상태 변경 요청 (생존 상태 간 전이). 사별은 전용 엔드포인트 사용")
public record ChangeElderStatusRequest(
        @Schema(description = "대상 상태", allowableValues = {"ACTIVE", "DECLINING", "HOSPITALIZED", "DORMANT"})
        @NotNull ElderStatus status
) {}
