package com.memeboo2.haemi.m2.presentation.dto.request;

import com.memeboo2.haemi.m2.domain.model.post.ReplyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "어르신 추억글 답변 요청 [F2-02] — 음성 우선, 마음 이모지 보조")
public record ReplyToPostRequest(
        @Schema(description = "어르신 memberId", example = "elder-001")
        @NotBlank String elderId,

        @Schema(description = "답변 유형", allowableValues = {"VOICE", "EMOJI"})
        @NotNull ReplyType replyType,

        @Schema(description = "마음 이모지 코드 (EMOJI 유형일 때). 6종: ❤️ 🤗 😊 🥹 🙏 🥰", example = "❤️")
        String heartEmojiCode
) {}
