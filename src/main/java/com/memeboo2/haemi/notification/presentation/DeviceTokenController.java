package com.memeboo2.haemi.notification.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import com.memeboo2.haemi.notification.application.DeviceTokenResult;
import com.memeboo2.haemi.notification.application.DeviceTokenService;
import com.memeboo2.haemi.notification.presentation.dto.RegisterDeviceTokenRequest;
import com.memeboo2.haemi.notification.presentation.dto.UnregisterDeviceTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Notification", description = "FCM 푸시 알림 기기 토큰 관리")
@RestController
@RequestMapping("/api/v1/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokens;

    @Operation(
            summary = "기기 알림 토큰 등록 [#80]",
            description = """
                    FCM 등록 토큰을 저장합니다. 토큰 소유자는 요청 본문이 아니라 인증 주체로 결정됩니다.
                    같은 토큰을 다시 등록하면 소유자와 사용 시각이 갱신됩니다(기기 재로그인).
                    어르신 본인 휴대전화라면 elderId를 함께 보내 Mode A/B 모두 해당 기기로 알림을 받을 수 있어요.
                    """
    )
    @PostMapping
    public ApiResponse<DeviceTokenResult> register(@AuthenticationPrincipal AuthenticatedMember member,
                                                   @Valid @RequestBody RegisterDeviceTokenRequest request) {
        DeviceTokenResult result = deviceTokens.register(
                member.memberId().toString(), request.token(), request.platform(), request.elderId());
        return ApiResponse.ok(result, "이 기기로 알림을 받을 수 있어요.");
    }

    @Operation(
            summary = "기기 알림 토큰 해지 [#80]",
            description = "로그아웃 등으로 더는 알림을 받지 않을 때 호출합니다. 본인 소유 토큰만 해지할 수 있습니다."
    )
    @DeleteMapping
    public ApiResponse<Void> unregister(@AuthenticationPrincipal AuthenticatedMember member,
                                        @Valid @RequestBody UnregisterDeviceTokenRequest request) {
        deviceTokens.unregister(member.memberId().toString(), request.token());
        return ApiResponse.ok(null, "이 기기로는 알림을 보내지 않아요.");
    }

    @Operation(summary = "내 기기 알림 토큰 목록 [#80]")
    @GetMapping
    public ApiResponse<List<DeviceTokenResult>> myTokens(@AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.ok(deviceTokens.findMyTokens(member.memberId().toString()));
    }
}
