package com.memeboo2.haemi.m0.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "접근 모드 진단 요청 (5문항, 각 0~2점)")
public record AssessAccessModeRequest(
        @Schema(description = "5문항 점수 (각 0~2)", example = "[2, 1, 2, 0, 1]")
        @NotNull @Size(min = 5, max = 5) List<Integer> answers
) {}
