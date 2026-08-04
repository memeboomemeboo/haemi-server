package com.memeboo2.haemi.m2.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m2.application.dto.GroupGoalResult;
import com.memeboo2.haemi.m2.application.dto.HighlightCardResult;
import com.memeboo2.haemi.m2.application.query.GetCurrentGoalQuery;
import com.memeboo2.haemi.m2.application.query.GetHighlightCardQuery;
import com.memeboo2.haemi.m2.application.service.GroupGoalApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "M2-GroupGoal", description = "F1-03-A 그룹 협력 목표 (개인 순위·뱃지·스트릭 없음)")
@RestController
@RequestMapping("/api/v1/albums/{albumId}/group-goal")
@RequiredArgsConstructor
public class GroupGoalController {

    private final GroupGoalApplicationService groupGoalService;

    @Operation(
            summary = "그룹 협력 목표 조회 [F1-03-A]",
            description = """
                    가족 전체가 함께 채우는 주간 협력 목표를 조회합니다.
                    개인 순위·뱃지·스트릭은 없으며, 그룹 전체의 공동 진척과 함께한 참여자 수만 제공합니다.

                    진행 중 목표가 없으면 이번 주 목표가 자동으로 시작됩니다.
                    진척 신호: 추억글 게시(+1), 어르신 답변(+1).
                    """
    )
    @GetMapping
    public ApiResponse<GroupGoalResult> getCurrentGoal(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId) {
        return ApiResponse.ok(groupGoalService.getCurrentGoal(new GetCurrentGoalQuery(albumId)));
    }

    @Operation(
            summary = "기간 하이라이트 카드 [F1-03-A]",
            description = """
                    이번 기간 동안 가족이 함께 이룬 성취를 축하 카드로 보여줍니다.
                    총 추억글 수, 어르신 답변 수, 총 좋아요 수와 '가족이 가장 사랑한 추억' 한 건을 담습니다.
                    개인 랭킹이 아니라 그룹 전체 관점의 하이라이트입니다.
                    """
    )
    @GetMapping("/highlight")
    public ApiResponse<HighlightCardResult> getHighlightCard(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId) {
        return ApiResponse.ok(groupGoalService.getHighlightCard(new GetHighlightCardQuery(albumId)));
    }
}
