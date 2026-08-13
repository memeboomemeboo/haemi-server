package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.dto.InstitutionAssignmentResult;
import com.memeboo2.haemi.m0.application.service.InstitutionAssignmentApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.CreateInstitutionAssignmentRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/elders/{elderId}/institution-assignments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('FAMILY')")
public class InstitutionAssignmentController {

    private final InstitutionAssignmentApplicationService assignments;

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

    @GetMapping
    public ApiResponse<List<InstitutionAssignmentResult>> findActive(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId
    ) {
        return ApiResponse.ok(assignments.findActive(member.memberId(), elderId));
    }

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
