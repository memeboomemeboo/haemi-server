package com.memeboo2.haemi.offline.presentation;

import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.offline.application.BatchIngestResult;
import com.memeboo2.haemi.offline.application.OfflineResultIngestService;
import com.memeboo2.haemi.offline.domain.OfflineSessionResult;
import com.memeboo2.haemi.offline.presentation.dto.OfflineResultRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Offline-Sync", description = "오프라인 세션 완주 결과 로컬 큐 동기화")
@RestController
@RequestMapping("/api/v1/offline-results")
@RequiredArgsConstructor
public class OfflineResultController {

    private final OfflineResultIngestService ingestService;

    @Operation(
            summary = "오프라인 완주 결과 동기화",
            description = """
                    단말 로컬 큐(7일 보관)의 오프라인 완주 결과를 배치로 전송합니다.
                    같은 idempotencyKey 재전송은 멱등하게 1회만 반영됩니다.
                    """
    )
    @PostMapping
    public ApiResponse<BatchIngestResult> sync(@RequestBody @Valid List<OfflineResultRequest> requests) {
        List<OfflineSessionResult> results = requests.stream()
                .map(OfflineResultRequest::toResult)
                .toList();
        return ApiResponse.ok(ingestService.ingestBatch(results), "동기화되었습니다.");
    }
}
