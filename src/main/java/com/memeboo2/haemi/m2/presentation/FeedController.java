package com.memeboo2.haemi.m2.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.application.dto.FeedResult;
import com.memeboo2.haemi.m2.application.query.GetFeedQuery;
import com.memeboo2.haemi.m2.application.service.MemoryPostApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "M2-Feed", description = "F2-04 공동 피드")
@RestController
@RequestMapping("/api/v1/albums/{albumId}/feed")
@RequiredArgsConstructor
public class FeedController {

    private final MemoryPostApplicationService postService;

    @Operation(
            summary = "공동 피드 조회 [F2-04]",
            description = """
                    가족 모두의 추억글이 모이는 공동 피드를 조회합니다.

                    **정렬:**
                    - `LATEST` (기본값): 최신 게시 순
                    - `POPULAR`: 인기도 순 (어르신 답변 ×3 + 좋아요 ×1)

                    **기간 필터:**
                    - `ALL` (기본값): 전체
                    - `WEEKLY`: 최근 7일
                    - `MONTHLY`: 최근 30일
                    """
    )
    @GetMapping
    public ApiResponse<FeedResult> getFeed(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "정렬", schema = @Schema(allowableValues = {"LATEST", "POPULAR"}))
            @RequestParam(defaultValue = "LATEST") String sortBy,
            @Parameter(description = "기간 필터", schema = @Schema(allowableValues = {"ALL", "WEEKLY", "MONTHLY"}))
            @RequestParam(defaultValue = "ALL") String period,
            @Parameter(description = "페이지 번호 (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(postService.getFeed(
                new GetFeedQuery(albumId, sortBy, period, page, size)));
    }
}
