package com.memeboo2.haemi.auth.application.service;

import com.memeboo2.haemi.auth.application.command.*;
import com.memeboo2.haemi.auth.application.dto.MemberResult;
import com.memeboo2.haemi.auth.application.dto.TokenResult;
import com.memeboo2.haemi.auth.application.command.BeginTotpEnrollmentCommand;
import com.memeboo2.haemi.auth.application.command.CompleteTotpEnrollmentCommand;
import com.memeboo2.haemi.auth.application.dto.TotpSetupResult;
import com.memeboo2.haemi.auth.domain.model.*;
import com.memeboo2.haemi.auth.domain.port.PasswordEncoderPort;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.port.TotpPort;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.auth.infrastructure.security.InstitutionAdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthApplicationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenPort tokenPort;
    private final TotpPort totpPort;
    private final InstitutionAdminProperties institutionAdminProperties;

    // ─────────── 회원가입 ───────────

    public MemberResult signUp(SignUpCommand cmd) {
        String normalizedEmail = cmd.email().trim().toLowerCase();
        requireSignUpAllowed(normalizedEmail, cmd.role());
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String encoded = passwordEncoder.encode(cmd.password());
        Member member = Member.create(normalizedEmail, encoded, cmd.name(), cmd.role());
        member = memberRepository.save(member);

        return MemberResult.from(member);
    }

    /**
     * 공개 가입 엔드포인트가 권한 역할을 그대로 받지 않게 한다 (#96).
     * 기관 관리자는 운영이 설정한 허용 목록의 이메일만 가입할 수 있다.
     */
    private void requireSignUpAllowed(String normalizedEmail, MemberRole role) {
        if (role != MemberRole.INSTITUTION_ADMIN) {
            return;
        }
        if (!institutionAdminProperties.normalizedAllowedEmails().contains(normalizedEmail)) {
            throw new InstitutionAdminSignUpNotAllowedException();
        }
    }

    // ─────────── 기관 관리자 2FA 최초 등록 ───────────

    /**
     * 2FA를 켜지 않은 기관 관리자는 로그인이 막히고, 2FA 설정 엔드포인트는 토큰을 요구한다.
     * 그 잠금을 자격증명으로 직접 여는 경로다 (#96).
     *
     * <p>대상을 {@code requiresTotp() && !totpEnabled}로 좁혀, 이미 2FA를 켠 계정이나 일반
     * 사용자가 이 경로로 2FA를 우회하거나 재설정할 수 없게 한다.
     */
    @Transactional(readOnly = true)
    public TotpSetupResult beginTotpEnrollment(BeginTotpEnrollmentCommand cmd) {
        Member member = authenticateForEnrollment(cmd.email(), cmd.password());
        String secret = totpPort.generateSecret();
        return new TotpSetupResult(secret, totpPort.generateQrUri(secret, member.getEmail()));
    }

    @Transactional
    public void completeTotpEnrollment(CompleteTotpEnrollmentCommand cmd) {
        Member member = authenticateForEnrollment(cmd.email(), cmd.password());
        if (!totpPort.verifyCode(cmd.secret(), cmd.code())) {
            throw new TotpRequiredException("인증 코드가 올바르지 않습니다. 다시 시도해주세요.");
        }
        member.enableTotp(cmd.secret());
        memberRepository.save(member);
    }

    private Member authenticateForEnrollment(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (!member.isActive()) {
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(password, member.getEncodedPassword())) {
            throw new InvalidCredentialsException();
        }
        // 잠긴 기관 관리자 전용이다. 그 외 계정은 인증된 /totp/setup 경로를 쓴다.
        if (!member.requiresTotp() || member.isTotpEnabled()) {
            throw new InvalidCredentialsException();
        }
        return member;
    }

    // ─────────── 로그인 ───────────

    public TokenResult login(LoginCommand cmd) {
        String normalizedEmail = cmd.email().trim().toLowerCase();
        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!member.isActive()) {
            if (member.getStatus() == MemberStatus.WITHDRAWN) {
                throw new InvalidCredentialsException();
            }
            throw new AccountSuspendedException();
        }

        if (!passwordEncoder.matches(cmd.password(), member.getEncodedPassword())) {
            throw new InvalidCredentialsException();
        }

        // 2FA 검증 (기관 관리자 필수, 일반 사용자는 설정된 경우만)
        if (member.isTotpEnabled()) {
            if (cmd.totpCode() == null || cmd.totpCode().isBlank()) {
                throw new TotpRequiredException("2FA 인증 코드를 입력해주세요.");
            }
            if (!totpPort.verifyCode(member.getTotpSecret(), cmd.totpCode())) {
                throw new TotpRequiredException("2FA 인증 코드가 올바르지 않습니다.");
            }
        } else if (member.requiresTotp()) {
            throw new TotpRequiredException("기관 관리자는 2FA 설정이 필요합니다. 로그인 후 2FA를 먼저 설정해주세요.");
        }

        // 토큰 발급
        String accessToken  = tokenPort.generateAccessToken(member.getId(), member.getEmail(), member.getRole());
        String refreshToken = tokenPort.generateRefreshToken(member.getId());

        member.updateRefreshTokenHash(tokenPort.hashToken(refreshToken));
        memberRepository.save(member);

        return new TokenResult(
                member.getId(), member.getEmail(), member.getName(),
                member.getRole(), accessToken, refreshToken,
                member.isTotpEnabled()
        );
    }

    // ─────────── 토큰 갱신 ───────────

    public TokenResult refresh(RefreshTokenCommand cmd) {
        if (!tokenPort.isValid(cmd.refreshToken())) {
            throw new InvalidRefreshTokenException();
        }

        UUID memberId = tokenPort.extractMemberId(cmd.refreshToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!member.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        // 저장된 해시와 비교 (RTR: Refresh Token Rotation)
        String incomingHash = tokenPort.hashToken(cmd.refreshToken());
        if (member.getRefreshTokenHash() == null
                || !member.getRefreshTokenHash().equals(incomingHash)) {
            // 탈취 의심 → 모든 세션 무효화
            member.clearRefreshToken();
            memberRepository.save(member);
            throw new InvalidRefreshTokenException();
        }

        // 새 토큰 쌍 발급
        String newAccess  = tokenPort.generateAccessToken(member.getId(), member.getEmail(), member.getRole());
        String newRefresh = tokenPort.generateRefreshToken(member.getId());
        member.updateRefreshTokenHash(tokenPort.hashToken(newRefresh));
        memberRepository.save(member);

        return new TokenResult(
                member.getId(), member.getEmail(), member.getName(),
                member.getRole(), newAccess, newRefresh,
                member.isTotpEnabled()
        );
    }

    // ─────────── 로그아웃 ───────────

    public void logout(UUID memberId, String accessToken) {
        tokenPort.blacklistAccessToken(accessToken);
        memberRepository.findById(memberId).ifPresent(member -> {
            member.clearRefreshToken();
            memberRepository.save(member);
        });
    }

    // ─────────── 비밀번호 변경 ───────────

    public void changePassword(ChangePasswordCommand cmd, String accessToken) {
        Member member = memberRepository.findById(cmd.memberId())
                .orElseThrow(() -> new MemberNotFoundException(cmd.memberId().toString()));

        if (!passwordEncoder.matches(cmd.currentPassword(), member.getEncodedPassword())) {
            throw new InvalidCredentialsException();
        }

        tokenPort.blacklistAccessToken(accessToken);
        member.changePassword(passwordEncoder.encode(cmd.newPassword()));
        member.clearRefreshToken();
        memberRepository.save(member);
    }

    // ─────────── 회원 탈퇴 ───────────

    public void withdraw(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId.toString()));
        member.withdraw();
        memberRepository.save(member);
    }

    // ─────────── 프로필 조회 ───────────

    @Transactional(readOnly = true)
    public MemberResult getProfile(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId.toString()));
        return MemberResult.from(member);
    }

    // ─────────── 2FA 설정 ───────────

    public TotpSetupResult enableTotp(EnableTotpCommand cmd) {
        Member member = memberRepository.findById(cmd.memberId())
                .orElseThrow(() -> new MemberNotFoundException(cmd.memberId().toString()));

        String secret = totpPort.generateSecret();
        String qrUri  = totpPort.generateQrUri(secret, member.getEmail());

        // 아직 저장하지 않음 — verifyTotp()에서 코드 검증 후 확정
        return new TotpSetupResult(secret, qrUri);
    }

    public void verifyAndActivateTotp(VerifyTotpCommand cmd, String secret) {
        Member member = memberRepository.findById(cmd.memberId())
                .orElseThrow(() -> new MemberNotFoundException(cmd.memberId().toString()));

        if (!totpPort.verifyCode(secret, cmd.code())) {
            throw new TotpRequiredException("인증 코드가 올바르지 않습니다. 다시 시도해주세요.");
        }

        member.enableTotp(secret);
        memberRepository.save(member);
    }

    public void disableTotp(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId.toString()));
        member.disableTotp();
        memberRepository.save(member);
    }
}
