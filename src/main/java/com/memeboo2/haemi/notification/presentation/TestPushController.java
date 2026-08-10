package com.memeboo2.haemi.notification.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.notification.application.PushDispatchService;
import com.memeboo2.haemi.notification.domain.PushMessage;
import com.memeboo2.haemi.notification.domain.PushSendResult;
import com.memeboo2.haemi.notification.presentation.dto.TestPushRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 개발 환경 전용 테스트 발송 (#80).
 *
 * <p>운영(prod) 프로필에서는 빈 자체가 뜨지 않는다. 발송 대상은 항상 요청한 본인의 기기로 고정해
 * 임의의 사용자에게 알림을 쏘는 통로가 되지 않게 한다.
 */
@Profile("dev")
@Tag(name = "Notification", description = "FCM 푸시 알림 기기 토큰 관리")
@RestController
@RequestMapping("/api/v1/device-tokens/test-send")
@RequiredArgsConstructor
public class TestPushController {

    private final PushDispatchService pushDispatch;

    @Operation(
            summary = "테스트 알림 발송 (dev 전용) [#80]",
            description = """
                    로그인한 본인의 등록 기기로 알림을 즉시 발송하고 결과를 돌려줍니다.
                    업무 흐름과 달리 동기로 발송하므로 성공·실패 건수를 응답에서 바로 확인할 수 있습니다.
                    운영 프로필에서는 이 엔드포인트가 존재하지 않습니다.
                    """
    )
    @PostMapping
    public ApiResponse<Map<String, Object>> sendToSelf(@AuthenticationPrincipal AuthenticatedMember member,
                                                       @Valid @RequestBody TestPushRequest request) {
        PushSendResult result = pushDispatch.dispatchToMember(
                member.memberId().toString(),
                new PushMessage(request.title(), request.body(), request.data()));

        String message = describe(result);
        return ApiResponse.ok(Map.of(
                "successCount", result.successCount(),
                "failureCount", result.failureCount(),
                "invalidTokens", result.invalidTokens()
        ), message);
    }

    // 기기가 없어서 안 간 것과, 기기는 있었는데 실패한 것은 원인이 전혀 다르다.
    private String describe(PushSendResult result) {
        if (result.successCount() > 0) {
            return "%d대에 발송했어요.".formatted(result.successCount());
        }
        if (result.failureCount() > 0) {
            String pruned = result.invalidTokens().isEmpty()
                    ? "일시 오류로 보여 토큰은 남겨뒀어요. 잠시 후 다시 시도해 보세요."
                    : "만료·무효 토큰 %d건은 정리했어요. 기기에서 토큰을 다시 등록하세요.".formatted(result.invalidTokens().size());
            return "%d대 모두 발송에 실패했어요. %s".formatted(result.failureCount(), pruned);
        }
        return "발송된 기기가 없어요. 토큰을 먼저 등록했는지, 자격증명이 설정됐는지 확인하세요.";
    }
}
