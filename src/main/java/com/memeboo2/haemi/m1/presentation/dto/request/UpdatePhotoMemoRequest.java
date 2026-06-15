package com.memeboo2.haemi.m1.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "사진 메모 & 회상 태깅 요청 [F1-04]")
public record UpdatePhotoMemoRequest(
        @Schema(description = "시기 (연도·계절)", example = "1978년 여름")
        String timePeriod,

        @Schema(description = "장소 (최대 100자)", example = "경상북도 안동 하회마을")
        @Size(max = 100) String locationText,

        @Schema(description = "추억 메모 (최대 200자)", example = "딸이랑 오리백숙 먹었다. 슴슴허니 맛이 좋~았다.")
        @Size(max = 200) String memo,

        @Schema(description = "인물 태그 목록 (최대 10명)")
        List<PersonTagRequest> personTags
) {
    @Schema(description = "인물 태그")
    public record PersonTagRequest(
            @Schema(description = "멤버 ID", example = "member-002")
            @NotBlank String memberId,

            @Schema(description = "이름/관계", example = "큰딸 영희")
            @NotBlank String memberName
    ) {}
}
