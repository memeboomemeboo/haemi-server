package com.memeboo2.haemi.m3.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.m3.application.command.UpdateDifficultyPolicyCommand;
import com.memeboo2.haemi.m3.application.dto.DifficultyPolicyResult;
import com.memeboo2.haemi.m3.application.service.DifficultyPolicyApplicationService;
import com.memeboo2.haemi.m3.presentation.dto.request.UpdateDifficultyPolicyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "M3-Difficulty-Policy", description = "F3-02 전문가 난이도 기준표 관리")
@RestController
@RequestMapping("/api/v1/admin/training/difficulty-policies")
@RequiredArgsConstructor
public class DifficultyPolicyAdminController {

    private final DifficultyPolicyApplicationService service;

    @Operation(summary = "난이도 기준표 전체 조회 [F3-02]")
    @GetMapping
    public ApiResponse<List<DifficultyPolicyResult>> getPolicies() {
        return ApiResponse.ok(service.getPolicies());
    }

    @Operation(summary = "난이도 기준표 검토·갱신 [F3-02]")
    @PutMapping("/{level}")
    public ApiResponse<DifficultyPolicyResult> updatePolicy(
            @PathVariable int level,
            @Valid @RequestBody UpdateDifficultyPolicyRequest request,
            @AuthenticationPrincipal AuthenticatedMember reviewer
    ) {
        DifficultyPolicyResult result = service.updatePolicy(new UpdateDifficultyPolicyCommand(
                level,
                request.maxAverageResponseSeconds(),
                request.increaseAccuracyThreshold(),
                request.decreaseAccuracyThreshold(),
                request.questionTypes(),
                reviewer.email(),
                request.reviewedDate()
        ));
        return ApiResponse.ok(result, "난이도 기준표가 갱신되었습니다.");
    }
}
