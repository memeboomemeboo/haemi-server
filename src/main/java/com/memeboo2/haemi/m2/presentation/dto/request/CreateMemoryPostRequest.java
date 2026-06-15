package com.memeboo2.haemi.m2.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "추억글 작성 요청 [F2-01]")
public record CreateMemoryPostRequest(
        @Schema(description = "작성자 memberId", example = "member-002")
        @NotBlank String memberId,

        @Schema(description = "작성자 이름", example = "김영희")
        @NotBlank String memberName,

        @Schema(description = "가족 관계", example = "딸")
        @NotBlank String relation,

        @Schema(description = "본문 텍스트 (최대 500자, 사진·음성 없으면 필수)",
                example = "아버지, 오늘 아버지가 좋아하시던 오리백숙을 먹었어요. 생각나서 사진 찍어 보내드려요.")
        @Size(max = 500) String textContent,

        @Schema(description = "즉시 게시 여부 (false면 임시 저장)", example = "true")
        boolean publishImmediately
) {}
