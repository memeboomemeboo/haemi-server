package com.memeboo2.haemi.m3.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m3.application.command.*;
import com.memeboo2.haemi.m3.application.dto.AnswerResult;
import com.memeboo2.haemi.m3.application.dto.ChanceResult;
import com.memeboo2.haemi.m3.application.dto.TrainingSessionResult;
import com.memeboo2.haemi.m3.application.query.GetTodayTrainingSessionQuery;
import com.memeboo2.haemi.m3.application.service.TrainingApplicationService;
import com.memeboo2.haemi.m3.presentation.dto.request.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "M3-Training", description = "F3-01 일일 인지 훈련 / F3-02 난이도 적응 / F3-03 손주 찬스")
@RestController
@RequestMapping("/api/v1/training/sessions")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingApplicationService trainingService;

    @Operation(
            summary = "일일 인지 훈련 세션 시작 [F3-01]",
            description = """
                    3~5개 문제로 구성된 오늘의 인지 훈련 세션을 시작합니다.
                    기억 앨범에 요청 어르신 프로필과 사진 5장 이상이 등록되어 있어야 합니다.
                    신규 사용자는 난이도 2에서 시작하며, 같은 유형 문제는 연속 배치되지 않습니다.
                    응답의 speechGuide는 문제 또는 완료 칭찬을 재생할 수 있는 한국어 TTS SSML입니다.
                    문제 생성 서비스 오류 시 최근 완료 세션의 문제 세트로 대체합니다.
                    """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TrainingSessionResult> startSession(
            @Valid @RequestBody StartTrainingSessionRequest request) {
        TrainingSessionResult result = trainingService.startSession(new StartTrainingSessionCommand(
                request.elderId(), request.albumId(), request.startMode()));
        return ApiResponse.ok(result, "오늘의 훈련을 시작합니다.");
    }

    @Operation(summary = "당일 훈련 이어서 조회 [F3-01]")
    @GetMapping("/today")
    public ApiResponse<TrainingSessionResult> getTodaySession(@RequestParam String elderId) {
        return ApiResponse.ok(trainingService.getTodaySession(new GetTodayTrainingSessionQuery(elderId)));
    }

    @Operation(
            summary = "문제 답변 제출 [F3-01/F3-02]",
            description = "정오답과 반응 시간을 기록합니다. 세션 완료 시 난이도 프로필을 자동 조정합니다."
    )
    @PostMapping("/{sessionId}/answers")
    public ApiResponse<AnswerResult> answer(
            @PathVariable String sessionId,
            @Valid @RequestBody AnswerTrainingQuestionRequest request) {
        return ApiResponse.ok(trainingService.answerQuestion(new AnswerTrainingQuestionCommand(
                sessionId, request.questionId(), request.submittedAnswer(), request.responseSeconds())));
    }

    @Operation(
            summary = "손주 찬스 요청 [F3-03]",
            description = "세션당 최대 2회까지 가족에게 힌트를 요청합니다."
    )
    @PostMapping("/{sessionId}/chances")
    public ApiResponse<ChanceResult> requestChance(
            @PathVariable String sessionId,
            @Valid @RequestBody RequestGrandchildChanceRequest request) {
        return ApiResponse.ok(trainingService.requestGrandchildChance(
                new RequestGrandchildChanceCommand(sessionId, request.elderId())));
    }

    @Operation(summary = "가족 힌트 전달 [F3-03]")
    @PostMapping("/{sessionId}/hints")
    public ApiResponse<TrainingSessionResult> provideHint(
            @PathVariable String sessionId,
            @Valid @RequestBody ProvideHintRequest request) {
        TrainingSessionResult result = trainingService.provideHint(new ProvideHintCommand(
                sessionId, request.responderMemberId(), request.responderName(), request.hintText()));
        return ApiResponse.ok(result, "힌트를 전달했습니다.");
    }
}
