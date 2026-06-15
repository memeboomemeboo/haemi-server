package com.memeboo2.haemi.m2.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.application.command.ComputeRankingCommand;
import com.memeboo2.haemi.m2.application.dto.RankingResult;
import com.memeboo2.haemi.m2.application.query.GetRankingQuery;
import com.memeboo2.haemi.m2.application.service.RankingApplicationService;
import com.memeboo2.haemi.m2.domain.model.ranking.RankingPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Tag(name = "M2-Ranking", description = "F2-04 가족 랭킹 & 뱃지")
@RestController
@RequestMapping("/api/v1/albums/{albumId}/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingApplicationService rankingService;

    @Operation(
            summary = "랭킹 조회 [F2-04]",
            description = """
                    가족 구성원별 주간 또는 월간 랭킹을 조회합니다.

                    **뱃지 등급:**
                    | 등급 | 누적 별 |
                    |------|--------|
                    | 사랑 새싹 | 1~4개 |
                    | 추억왕 | 5~9개 |
                    | 추억킹 | 10~19개 |
                    | 추억 갓 | 20개+ |

                    **점수 계산:** 어르신 답변(×3) + 가족 좋아요(×1)
                    동점 시: 어르신 답변 수 → 게시 수 → 빠른 게시 순
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (아직 집계 전이면 data: null)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @GetMapping
    public ApiResponse<RankingResult> getRanking(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "집계 기간", schema = @Schema(allowableValues = {"WEEKLY", "MONTHLY"}))
            @RequestParam(defaultValue = "WEEKLY") RankingPeriod period) {
        Optional<RankingResult> result = rankingService.getRanking(new GetRankingQuery(albumId, period));
        return result.map(ApiResponse::ok)
                .orElse(ApiResponse.ok(null, "아직 집계된 랭킹이 없습니다."));
    }

    @Operation(
            summary = "랭킹 수동 집계 [F2-04]",
            description = "지정 기간의 랭킹을 즉시 집계합니다. (관리자·테스트 용도)"
    )
    @PostMapping("/compute")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RankingResult> computeRanking(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId,
            @Parameter(description = "집계 기간") @RequestParam(defaultValue = "WEEKLY") RankingPeriod period,
            @Parameter(description = "기간 시작일 (yyyy-MM-dd)", example = "2025-06-09")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "기간 종료일 (yyyy-MM-dd)", example = "2025-06-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ApiResponse.ok(
                rankingService.computeRanking(
                        new ComputeRankingCommand(albumId, period, start, end)),
                "랭킹이 집계되었습니다.");
    }
}
