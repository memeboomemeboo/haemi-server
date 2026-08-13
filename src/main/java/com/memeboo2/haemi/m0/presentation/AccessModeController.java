package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.dto.AccessModeResult;
import com.memeboo2.haemi.m0.application.service.AccessModeApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.ApplyAccessModeRequest;
import com.memeboo2.haemi.m0.presentation.dto.request.AssessAccessModeRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "M0-AccessMode", description = "F0-03 접근 모드(Mode A/B) 진단·추천·적용")
@RestController
@RequestMapping("/api/v1/elders/{elderId}/access-mode")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class AccessModeController {

    private final AccessModeApplicationService accessModeService;

    @Operation(summary = "접근 모드 진단 제출 → 추천 제안 [F0-03]")
    @PostMapping("/assess")
    public ApiResponse<AccessModeResult> assess(@AuthenticationPrincipal AuthenticatedMember member,
                                                @PathVariable UUID elderId,
                                                @Valid @RequestBody AssessAccessModeRequest request) {
        return ApiResponse.ok(accessModeService.assess(member.memberId(), elderId, request.answers()),
                "진단 결과 추천을 제안했어요.");
    }

    @Operation(summary = "접근 모드 추천 적용 (데이터 승계·대행 기록) [F0-03]")
    @PostMapping("/recommendations/{recommendationId}/apply")
    public ApiResponse<AccessModeResult> apply(@AuthenticationPrincipal AuthenticatedMember member,
                                               @PathVariable UUID elderId,
                                               @PathVariable UUID recommendationId,
                                               @Valid @RequestBody ApplyAccessModeRequest request) {
        return ApiResponse.ok(accessModeService.applyRecommendation(
                member.memberId(), elderId, recommendationId, request.entryPath(), member.memberId()),
                "접근 모드를 적용했어요.");
    }

    @Operation(summary = "접근 모드 확정 또는 변경 [F0-03]")
    @PutMapping
    public ApiResponse<AccessModeResult> applyV3(@AuthenticationPrincipal AuthenticatedMember member,
                                                  @PathVariable UUID elderId,
                                                  @RequestParam UUID recommendationId,
                                                  @Valid @RequestBody ApplyAccessModeRequest request) {
        return apply(member, elderId, recommendationId, request);
    }

    @Operation(summary = "접근 모드 추천 기각 [F0-03]")
    @PostMapping("/recommendations/{recommendationId}/dismiss")
    public ApiResponse<AccessModeResult> dismiss(@AuthenticationPrincipal AuthenticatedMember member,
                                                 @PathVariable UUID elderId,
                                                 @PathVariable UUID recommendationId) {
        return ApiResponse.ok(accessModeService.dismissRecommendation(member.memberId(), elderId, recommendationId));
    }

    @Operation(summary = "최신 접근 모드 추천 조회 [F0-03]")
    @GetMapping
    public ApiResponse<AccessModeResult> getLatest(@AuthenticationPrincipal AuthenticatedMember member,
                                                   @PathVariable UUID elderId) {
        return ApiResponse.ok(accessModeService.getLatest(member.memberId(), elderId));
    }
}
