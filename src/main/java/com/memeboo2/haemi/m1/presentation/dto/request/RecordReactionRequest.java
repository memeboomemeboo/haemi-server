package com.memeboo2.haemi.m1.presentation.dto.request;

import com.memeboo2.haemi.m1.domain.model.reminiscence.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "어르신 반응 기록 요청 [F1-05]")
public record RecordReactionRequest(
        @Schema(description = "어르신 memberId", example = "elder-001")
        @NotBlank String elderId,

        @Schema(description = "반응 유형", allowableValues = {"LIKE", "HAPPY", "SAD", "SURPRISED", "NOSTALGIC"})
        @NotNull ReactionType reaction
) {}
