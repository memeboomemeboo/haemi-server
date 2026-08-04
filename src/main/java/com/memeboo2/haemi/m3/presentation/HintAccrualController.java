package com.memeboo2.haemi.m3.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m3.application.command.AccrueHintCommand;
import com.memeboo2.haemi.m3.application.dto.AccruedHintResult;
import com.memeboo2.haemi.m3.application.service.TrainingApplicationService;
import com.memeboo2.haemi.m3.presentation.dto.request.AccrueHintRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "M3-Hint", description = "F3-03 손주 한마디 사전 적립")
@RestController
@RequestMapping("/api/v1/training/hints")
@RequiredArgsConstructor
public class HintAccrualController {

    private final TrainingApplicationService trainingService;

    @Operation(
            summary = "손주 한마디 적립 [F3-03]",
            description = """
                    가족이 손주 한마디를 미리 적립합니다. 적립 경로(source)는
                    메모(MEMO)/온보딩(ONBOARDING)/주간 리마인더(WEEKLY_REMINDER)/반응 유도(REACTION)입니다.
                    photoId가 있으면 특정 사진(L1), 없으면 어르신 일반(L2) 힌트로 저장됩니다.
                    """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccruedHintResult> accrue(@Valid @RequestBody AccrueHintRequest request) {
        AccruedHintResult result = trainingService.accrueHint(new AccrueHintCommand(
                request.elderId(), request.photoId(), request.personName(), request.source(),
                request.authorMemberId(), request.authorName(), request.text()));
        return ApiResponse.ok(result, "손주 한마디를 적립했습니다.");
    }
}
