package com.memeboo2.haemi.m1.presentation;

import com.memeboo2.haemi.m1.application.command.RecordReactionCommand;
import com.memeboo2.haemi.m1.application.dto.ReminiscenceResult;
import com.memeboo2.haemi.m1.application.query.GetTodayReminiscenceQuery;
import com.memeboo2.haemi.m1.application.service.ReminiscenceApplicationService;
import com.memeboo2.haemi.m1.presentation.dto.request.RecordReactionRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Tag(name = "M1-Reminiscence", description = "F1-05 AI 회상 콘텐츠 자동 생성")
@RestController
@RequiredArgsConstructor
public class ReminiscenceController {

    private final ReminiscenceApplicationService reminiscenceService;

    @Operation(
            summary = "오늘의 회상 조회 [F1-05]",
            description = """
                    오늘 생성된 AI 회상 콘텐츠를 반환합니다.

                    **콘텐츠 구성:**
                    - `cards`: 사진과 이야기 청유 문장으로 구성된 회상 카드 (3~5장)

                    매일 오전 08:00에 자동 생성됩니다.
                    앨범에 완료된 분석 사진이 20장 미만이거나 안전 검증을 통과한 카드가 3장 미만이면 `data: null`을 반환합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (data가 null이면 사진 부족 상태)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @GetMapping("/api/v1/albums/{albumId}/reminiscence/today")
    public ApiResponse<ReminiscenceResult> getTodayReminiscence(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId) {
        Optional<ReminiscenceResult> result = reminiscenceService.getTodayReminiscence(
                new GetTodayReminiscenceQuery(albumId));
        return result.map(ApiResponse::ok)
                .orElse(ApiResponse.ok(null, "사진을 조금 더 담아주시면 오늘의 회상이 시작됩니다."));
    }

    @Operation(
            summary = "회상 콘텐츠 수동 생성 [F1-05]",
            description = """
                    오늘의 회상 콘텐츠를 즉시 생성합니다. (관리자·테스트 용도)

                    - 당일 이미 생성된 콘텐츠가 있으면 재생성하지 않고 기존 콘텐츠를 반환합니다.
                    - 최근 7일 이내 노출된 사진은 제외하고 새로운 조합으로 생성합니다.
                    - 사별 인물 현재형, 금기 주제, 평가·퀴즈 어휘를 서버 안전 검증으로 차단합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "사진 부족 (data: null)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "앨범 없음")
    })
    @PostMapping("/api/v1/albums/{albumId}/reminiscence/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReminiscenceResult> generateReminiscence(
            @Parameter(description = "앨범 UUID") @PathVariable String albumId) {
        Optional<ReminiscenceResult> result = reminiscenceService.generateTodayReminiscence(albumId);
        return result.map(r -> ApiResponse.ok(r, "회상 콘텐츠가 생성되었습니다."))
                .orElse(ApiResponse.ok(null, "안전하게 보여드릴 회상 카드가 아직 부족합니다."));
    }

    @Operation(
            summary = "어르신 반응 기록 [F1-05]",
            description = """
                    어르신이 회상 콘텐츠를 보고 남긴 반응을 기록합니다.
                    기록된 반응은 AI 학습 데이터로 활용되어 콘텐츠 개인화에 반영됩니다.

                    **반응 유형:** `LIKE`, `HAPPY`, `SAD`, `SURPRISED`, `NOSTALGIC`
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "기록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "콘텐츠 없음")
    })
    @PostMapping("/api/v1/reminiscence/{contentId}/reaction")
    public ApiResponse<Void> recordReaction(
            @Parameter(description = "회상 콘텐츠 UUID") @PathVariable String contentId,
            @Valid @RequestBody RecordReactionRequest request) {
        reminiscenceService.recordReaction(
                new RecordReactionCommand(contentId, request.elderId(), request.reaction()));
        return ApiResponse.ok(null, "반응이 기록되었습니다.");
    }
}
