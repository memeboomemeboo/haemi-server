package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.dto.HomeContextResult;
import com.memeboo2.haemi.m0.application.service.HomeContextApplicationService;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "토큰 기반 홈 컨텍스트")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('FAMILY', 'ELDER')")
public class HomeController {

    private final HomeContextApplicationService homeContext;

    @Operation(summary = "홈 컨텍스트 조회", description = "토큰 주체에서 elderId와 groupId를 서버가 해석합니다.")
    @GetMapping
    public ApiResponse<HomeContextResult> get(@AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.ok(homeContext.resolve(member.memberId(), member.role()));
    }
}
