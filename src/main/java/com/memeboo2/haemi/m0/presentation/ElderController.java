package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.command.*;
import com.memeboo2.haemi.m0.application.dto.*;
import com.memeboo2.haemi.m0.application.service.ElderApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.*;
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

@Tag(name = "M0-Elder", description = "F0-02 어르신 프로필과 생애 정보")
@RestController
@RequestMapping("/api/v1/elders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
public class ElderController {

    private final ElderApplicationService elders;

    @Operation(summary = "어르신 프로필 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ElderResult> create(@AuthenticationPrincipal AuthenticatedMember member,
                                           @RequestParam UUID groupId,
                                           @Valid @RequestBody CreateElderRequest request) {
        return ApiResponse.ok(elders.create(member.memberId(), groupId,
                new CreateElderCommand(request.orgId(), request.name(), request.birthYear(), request.gender(),
                        request.residenceType())));
    }

    @Operation(summary = "어르신 프로필 및 별도 동의 건강 정보 수정")
    @PatchMapping("/{elderId}")
    public ApiResponse<ElderResult> update(@AuthenticationPrincipal AuthenticatedMember member,
                                           @PathVariable UUID elderId,
                                           @Valid @RequestBody UpdateElderRequest request) {
        return ApiResponse.ok(elders.update(member.memberId(), elderId,
                new UpdateElderCommand(request.orgId(), request.name(), request.birthYear(), request.gender(),
                        request.residenceType(), request.diagnosisLevel(), request.healthConsentId(),
                        request.diagnosedAt())));
    }

    @Operation(summary = "생애 정보 일괄 갱신")
    @PutMapping("/{elderId}/life-story")
    public ApiResponse<List<LifeStoryResult>> replaceLifeStories(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId,
            @Valid @RequestBody ReplaceLifeStoryRequest request) {
        List<LifeStoryItem> items = request.items().stream()
                .map(item -> new LifeStoryItem(item.category(), item.value(), item.weight(), item.source()))
                .toList();
        return ApiResponse.ok(elders.replaceLifeStories(member.memberId(), elderId, items));
    }

    @Operation(summary = "피해야 할 주제 등록", description = "콘텐츠 생성 안전 필터에 즉시 반영됩니다.")
    @PostMapping("/{elderId}/sensitive-topics")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SensitiveTopicResult> addSensitiveTopic(
            @AuthenticationPrincipal AuthenticatedMember member,
            @PathVariable UUID elderId,
            @Valid @RequestBody CreateSensitiveTopicRequest request) {
        return ApiResponse.ok(elders.addSensitiveTopic(member.memberId(), elderId, request.keyword(), request.reason()));
    }

    @Operation(summary = "생애 정보 완성도 조회")
    @GetMapping("/{elderId}/completeness")
    public ApiResponse<ElderCompletenessResult> completeness(@AuthenticationPrincipal AuthenticatedMember member,
                                                             @PathVariable UUID elderId) {
        return ApiResponse.ok(elders.completeness(member.memberId(), elderId));
    }

    @Operation(summary = "민감 건강 정보 동의 철회", description = "별도 테이블의 암호화된 건강 정보를 즉시 파기합니다.")
    @DeleteMapping("/{elderId}/health")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawHealthConsent(@AuthenticationPrincipal AuthenticatedMember member,
                                      @PathVariable UUID elderId) {
        elders.withdrawHealthConsent(member.memberId(), elderId);
    }

    @Operation(summary = "어르신 프로필 조회")
    @GetMapping("/{elderId}")
    public ApiResponse<ElderResult> get(@AuthenticationPrincipal AuthenticatedMember member,
                                        @PathVariable UUID elderId) {
        return ApiResponse.ok(elders.get(member.memberId(), elderId));
    }
}
