package com.memeboo2.haemi.m2.presentation.dto.request;

import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "어르신 추억글 답변 요청 [F2-03]")
public record ReplyToPostRequest(
        @Schema(description = "어르신 memberId", example = "elder-001")
        @NotBlank String elderId,

        @Schema(description = "답변 유형", allowableValues = {"POEM", "IMAGE", "SHORT_TEXT"})
        @NotNull ReplyType replyType,

        @Schema(description = "텍스트 답변 내용 (POEM: 최대 200자, SHORT_TEXT: 최대 100자)")
        String textContent,

        @Schema(description = "이미지 storageKey 또는 이모지 코드 (IMAGE 유형일 때)")
        String imageKeyOrEmoji
) {}
