package com.memeboo2.haemi.m0.presentation;

import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.m0.application.command.AcceptElderInvitationCommand;
import com.memeboo2.haemi.m0.application.command.RefreshElderSessionCommand;
import com.memeboo2.haemi.m0.application.dto.ElderSessionResult;
import com.memeboo2.haemi.m0.application.service.ElderJoinApplicationService;
import com.memeboo2.haemi.m0.presentation.dto.request.AcceptElderInvitationRequest;
import com.memeboo2.haemi.m0.presentation.dto.request.RefreshElderSessionRequest;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "M0-ElderJoin", description = "F0-01-E 어르신 참여 로그인과 평생 세션")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ElderJoinController {

    private final ElderJoinApplicationService elderJoin;

    @Operation(
            summary = "어르신 참여 [F0-01-E]",
            description = """
                    성함·전화번호·6자리 초대 코드를 검증해 가족 그룹의 어르신으로 연결하고 평생 세션을 발급합니다.
                    전화번호는 OTP 없이 식별·중복 감지용 해시로만 저장합니다.
                    입력 성함이 프로필과 다르면 합류를 보류하고 대표 보호자 확인을 요청합니다.
                    """
    )
    @PostMapping("/invitations/{code}/accept-elder")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ElderSessionResult> acceptElder(@PathVariable String code,
                                                       @Valid @RequestBody AcceptElderInvitationRequest request) {
        return ApiResponse.ok(elderJoin.join(new AcceptElderInvitationCommand(
                code, request.name(), request.phoneNumber(), request.deviceId())), "이제 들어가실 수 있어요.");
    }

    @Operation(
            summary = "어르신 세션 갱신 [F0-01-E]",
            description = "앱 실행 시 호출합니다. 사용할 때마다 만료가 연장되어 어르신에게 재로그인을 요구하지 않습니다."
    )
    @PostMapping("/elder-sessions/refresh")
    public ApiResponse<ElderSessionResult> refresh(@Valid @RequestBody RefreshElderSessionRequest request) {
        return ApiResponse.ok(elderJoin.refresh(
                new RefreshElderSessionCommand(request.refreshToken(), request.deviceId())));
    }

    @Operation(
            summary = "어르신 기기 연결 초기화 [F0-01-E]",
            description = "어르신 화면에는 로그아웃이 없으므로 대표 보호자만 세션을 폐기할 수 있습니다."
    )
    @DeleteMapping("/elders/{elderId}/sessions")
    @PreAuthorize("hasAnyRole('FAMILY', 'INSTITUTION_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal AuthenticatedMember member, @PathVariable UUID elderId) {
        elderJoin.revokeByOwner(member.memberId(), elderId);
    }
}
