package com.memeboo2.haemi.m1.presentation;

import com.memeboo2.haemi.m1.application.dto.MemoryFeedResult;
import com.memeboo2.haemi.m1.application.service.MemoryApplicationService;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Tag(name = "M1-ElderMemory", description = "어르신 기기용 안전한 추억 피드")
@RestController
@RequestMapping("/api/v1/elders/{elderId}/memories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ELDER')")
public class ElderMemoryFeedController {

    private final MemoryApplicationService memories;

    @Operation(summary = "어르신 추억 피드 조회", description = "GROUP_ALL 및 CLEAR 상태만 반환합니다. family_only는 선다운로드와 조회 모두에서 제외됩니다.")
    @GetMapping
    public ApiResponse<MemoryFeedResult> getFeed(@PathVariable UUID elderId,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(memories.getElderFeed(elderId, page, size));
    }
}
