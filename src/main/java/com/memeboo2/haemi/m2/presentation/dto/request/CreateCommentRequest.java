package com.memeboo2.haemi.m2.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "추억글 댓글 작성 요청")
public record CreateCommentRequest(
        @Schema(description = "작성자 memberId", example = "member-002")
        @NotBlank String memberId,

        @Schema(description = "작성자 이름", example = "김영희")
        @NotBlank String memberName,

        @Schema(description = "가족 관계", example = "딸")
        @NotBlank String relation,

        @Schema(description = "댓글 내용 (최대 200자)", example = "아버지 보고싶어요!")
        @NotBlank @Size(max = 200) String content
) {}
