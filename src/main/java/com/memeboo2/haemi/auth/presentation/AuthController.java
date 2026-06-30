package com.memeboo2.haemi.auth.presentation;

import com.memeboo2.haemi.auth.application.command.*;
import com.memeboo2.haemi.auth.application.dto.MemberResult;
import com.memeboo2.haemi.auth.application.dto.TokenResult;
import com.memeboo2.haemi.auth.application.dto.TotpSetupResult;
import com.memeboo2.haemi.auth.application.service.AuthApplicationService;
import com.memeboo2.haemi.auth.infrastructure.security.AuthenticatedMember;
import com.memeboo2.haemi.auth.presentation.dto.request.*;
import com.memeboo2.haemi.m1.presentation.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰 관리 · 2FA · 프로필")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authService;

    // ─────────── 회원가입 ───────────

    @Operation(
            summary = "회원가입",
            description = """
                    이메일·비밀번호·이름·역할로 회원가입합니다.

                    **역할:**
                    - `FAMILY` — 가족 (원격 참여자)
                    - `ELDER` — 치매 어르신
                    - `INSTITUTION_ADMIN` — 요양 기관 관리자 (2FA 필수)
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    })
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResult> signUp(@RequestBody @Valid SignUpRequest request) {
        MemberResult result = authService.signUp(new SignUpCommand(
                request.email(), request.password(), request.name(), request.role()
        ));
        return ApiResponse.ok(result, "회원가입이 완료되었습니다.");
    }

    // ─────────── 로그인 ───────────

    @Operation(
            summary = "로그인",
            description = """
                    이메일·비밀번호로 로그인합니다.
                    기관 관리자(INSTITUTION_ADMIN)는 2FA TOTP 코드가 필요합니다.

                    **응답에 포함되는 토큰:**
                    - `accessToken` — API 호출 시 `Authorization: Bearer {token}` 헤더에 사용 (30분)
                    - `refreshToken` — 액세스 토큰 만료 시 갱신용 (14일), RTR(Rotation) 적용
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "계정 정지"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "428", description = "2FA 코드 필요")
    })
    @PostMapping("/login")
    public ApiResponse<TokenResult> login(@RequestBody @Valid LoginRequest request) {
        TokenResult result = authService.login(new LoginCommand(
                request.email(), request.password(), request.totpCode()
        ));
        return ApiResponse.ok(result, "로그인 성공");
    }

    // ─────────── 토큰 갱신 ───────────

    @Operation(
            summary = "토큰 갱신 (RTR)",
            description = """
                    리프레시 토큰으로 새 액세스·리프레시 토큰 쌍을 발급합니다.
                    사용된 리프레시 토큰은 즉시 폐기됩니다 (Refresh Token Rotation).
                    탈취 감지 시 모든 세션이 무효화됩니다.
                    """
    )
    @PostMapping("/refresh")
    public ApiResponse<TokenResult> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        TokenResult result = authService.refresh(new RefreshTokenCommand(request.refreshToken()));
        return ApiResponse.ok(result, "토큰이 갱신되었습니다.");
    }

    // ─────────── 로그아웃 ───────────

    @Operation(summary = "로그아웃", description = "액세스 토큰을 즉시 무효화하고 리프레시 토큰을 폐기합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal AuthenticatedMember member,
            HttpServletRequest request) {
        authService.logout(member.memberId(), resolveToken(request));
        return ApiResponse.ok(null, "로그아웃되었습니다.");
    }

    // ─────────── 비밀번호 변경 ───────────

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호 확인 후 변경합니다. 변경 후 모든 세션이 만료됩니다.")
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestBody @Valid ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        authService.changePassword(
                new ChangePasswordCommand(member.memberId(), request.currentPassword(), request.newPassword()),
                resolveToken(httpRequest)
        );
        return ApiResponse.ok(null, "비밀번호가 변경되었습니다. 다시 로그인해주세요.");
    }

    // ─────────── 내 프로필 조회 ───────────

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ApiResponse<MemberResult> getProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.ok(authService.getProfile(member.memberId()));
    }

    // ─────────── 회원 탈퇴 ───────────

    @Operation(summary = "회원 탈퇴", description = "계정을 탈퇴 처리합니다. 복구할 수 없습니다.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal AuthenticatedMember member) {
        authService.withdraw(member.memberId());
    }

    // ─────────── 2FA 설정 ───────────

    @Operation(
            summary = "2FA TOTP 설정 시작",
            description = "비밀키와 QR URI를 발급합니다. 인증 앱(Google Authenticator 등)으로 QR 스캔 후 코드를 검증해주세요."
    )
    @PostMapping("/totp/setup")
    public ApiResponse<TotpSetupResult> setupTotp(
            @AuthenticationPrincipal AuthenticatedMember member) {
        return ApiResponse.ok(authService.enableTotp(new EnableTotpCommand(member.memberId())));
    }

    @Operation(summary = "2FA TOTP 활성화 확인", description = "설정 시 받은 비밀키와 인증 앱 코드를 검증하여 2FA를 활성화합니다.")
    @PostMapping("/totp/verify")
    public ApiResponse<Void> verifyTotp(
            @AuthenticationPrincipal AuthenticatedMember member,
            @RequestBody @Valid VerifyTotpRequest request) {
        authService.verifyAndActivateTotp(
                new VerifyTotpCommand(member.memberId(), request.code()),
                request.secret()
        );
        return ApiResponse.ok(null, "2FA가 활성화되었습니다.");
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Operation(summary = "2FA TOTP 비활성화")
    @DeleteMapping("/totp")
    public ApiResponse<Void> disableTotp(
            @AuthenticationPrincipal AuthenticatedMember member) {
        authService.disableTotp(member.memberId());
        return ApiResponse.ok(null, "2FA가 비활성화되었습니다.");
    }
}
