package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.dto.ElderStatusResult;
import com.memeboo2.haemi.m0.application.service.ElderStatusApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.ChangeElderStatusRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "M0-ElderStatus", description = "F0-05 어르신 상태 관리 및 사별 처리")
@RestController
@RequestMapping("/api/v1/elders/{elderId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class ElderStatusController {

    private final ElderStatusApplicationService statusService;

    @Operation(summary = "어르신 상태 조회 [F0-05]")
    @GetMapping("/status")
    public ApiResponse<ElderStatusResult> get(@AuthenticationPrincipal AuthenticatedMember member,
                                              @PathVariable UUID elderId) {
        return ApiResponse.ok(statusService.get(member.memberId(), elderId));
    }

    @Operation(summary = "어르신 상태 전이 (생존 상태 간) [F0-05]")
    @PatchMapping("/status")
    public ApiResponse<ElderStatusResult> changeStatus(@AuthenticationPrincipal AuthenticatedMember member,
                                                       @PathVariable UUID elderId,
                                                       @Valid @RequestBody ChangeElderStatusRequest request) {
        return ApiResponse.ok(statusService.changeStatus(member.memberId(), elderId, request.status()));
    }

    @Operation(summary = "사별 처리 요청 (1단계) [F0-05]")
    @PostMapping("/bereavement/request")
    public ApiResponse<ElderStatusResult> requestBereavement(@AuthenticationPrincipal AuthenticatedMember member,
                                                             @PathVariable UUID elderId) {
        return ApiResponse.ok(statusService.requestBereavement(member.memberId(), elderId),
                "사별 처리 확인이 필요해요.");
    }

    @Operation(summary = "사별 처리 확정 (2단계) [F0-05]",
            description = "확정 시 예약 잡 취소·기기 원격 잠금·7일 무음기간이 적용됩니다.")
    @PostMapping("/bereavement/confirm")
    public ApiResponse<ElderStatusResult> confirmBereavement(@AuthenticationPrincipal AuthenticatedMember member,
                                                             @PathVariable UUID elderId) {
        return ApiResponse.ok(statusService.confirmBereavement(member.memberId(), elderId),
                "사별 처리가 완료되었어요.");
    }

    @Operation(summary = "사별 오등록 복구 (48시간 내) [F0-05]")
    @PostMapping("/bereavement/recover")
    public ApiResponse<ElderStatusResult> recoverBereavement(@AuthenticationPrincipal AuthenticatedMember member,
                                                             @PathVariable UUID elderId) {
        return ApiResponse.ok(statusService.recoverBereavement(member.memberId(), elderId),
                "사별 처리가 복구되었어요.");
    }

    @Operation(summary = "memorial 기억 보관함 봉인 (무음기간 경과 후) [F0-05]")
    @PostMapping("/memorial")
    public ApiResponse<ElderStatusResult> enshrineMemorial(@AuthenticationPrincipal AuthenticatedMember member,
                                                           @PathVariable UUID elderId) {
        return ApiResponse.ok(statusService.enshrineMemorial(member.memberId(), elderId));
    }
}
