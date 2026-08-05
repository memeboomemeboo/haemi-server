package com.memeboo2.haemi.eventlog.presentation;

import com.memeboo2.haemi.eventlog.application.BatchEventResult;
import com.memeboo2.haemi.eventlog.application.EventLoggingService;
import com.memeboo2.haemi.eventlog.domain.EventEnvelope;
import com.memeboo2.haemi.eventlog.presentation.dto.EventIngestRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "EventLog", description = "F0-06 이벤트 로깅 및 지표 파이프라인")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventIngestController {

    private final EventLoggingService eventLoggingService;

    @Operation(
            summary = "이벤트 배치 수집 [F0-06]",
            description = """
                    단말 로컬 큐의 이벤트를 배치로 전송합니다. 같은 idempotencyKey 재전송은 멱등하게 1회만 수집됩니다.
                    동의 철회 상태의 어르신 이벤트는 수집되지 않습니다(rejected).
                    """
    )
    @PostMapping
    public ApiResponse<BatchEventResult> ingest(@RequestBody @Valid List<EventIngestRequest> requests) {
        List<EventEnvelope> envelopes = requests.stream()
                .map(EventIngestRequest::toEnvelope)
                .toList();
        return ApiResponse.ok(eventLoggingService.ingestBatch(envelopes));
    }

    @Operation(
            summary = "이벤트 수집 동의 철회 [F0-06]",
            description = "즉시 수집을 중단하고 기존 수집분의 어르신 식별자를 가명 처리합니다."
    )
    @PostMapping("/consent/{elderId}/withdraw")
    public ApiResponse<Integer> withdrawConsent(@PathVariable String elderId) {
        return ApiResponse.ok(eventLoggingService.withdrawConsent(elderId), "수집을 중단하고 기존분을 가명 처리했어요.");
    }
}
