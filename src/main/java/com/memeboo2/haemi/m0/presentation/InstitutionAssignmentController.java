package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.dto.InstitutionAssignmentResult;
import com.memeboo2.haemi.m0.application.service.InstitutionAssignmentApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.CreateInstitutionAssignmentRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "M0-InstitutionAssignment", description = "F0-02 어르신-기관 담당자 배정 관리 (가족 보호자 전용)")
@RestController
@RequestMapping("/api/v1/elders/{elderId}/institution-assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FAMILY')")
public class InstitutionAssignmentController {

    private final InstitutionAssignmentApplicationService assignments;

    @Operation(summary = "기관 담당자 배정", description = "가족 보호자가 어르신을 기관 담당자에게 배정합니다. 이미 배정된 담당자면 기존 배정이 유지됩니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InstitutionAssignmentResult> assign(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId,
            @RequestBody @Valid CreateInstitutionAssignmentRequest request
    ) {
        return ApiResponse.ok(assignments.assign(member.memberId(), elderId,
                request.institutionId(), request.institutionAdminMemberId()));
    }

    @Operation(summary = "활성 배정 목록 조회", description = "해당 어르신에 대해 현재 유효한 기관 담당자 배정을 모두 조회합니다.")
    @GetMapping
    public ApiResponse<List<InstitutionAssignmentResult>> findActive(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId
    ) {
        return ApiResponse.ok(assignments.findActive(member.memberId(), elderId));
    }

    @Operation(summary = "기관 담당자 배정 해제", description = "지정한 기관 담당자의 배정을 해제합니다. 해제 후 담당자는 해당 어르신 데이터에 접근할 수 없습니다.")
    @DeleteMapping("/{institutionAdminMemberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId,
            @PathVariable UUID institutionAdminMemberId
    ) {
        assignments.revoke(member.memberId(), elderId, institutionAdminMemberId);
    }
}
