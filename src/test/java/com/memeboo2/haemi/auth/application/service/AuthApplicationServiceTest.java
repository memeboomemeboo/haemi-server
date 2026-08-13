package com.memeboo2.haemi.auth.application.service;

import com.memeboo2.haemi.auth.application.command.ChangePasswordCommand;
import com.memeboo2.haemi.auth.application.command.LoginCommand;
import com.memeboo2.haemi.auth.application.command.RefreshTokenCommand;
import com.memeboo2.haemi.auth.application.command.SignUpCommand;
import com.memeboo2.haemi.auth.application.dto.MemberResult;
import com.memeboo2.haemi.auth.application.dto.TokenResult;
import com.memeboo2.haemi.auth.domain.model.*;
import com.memeboo2.haemi.auth.domain.port.PasswordEncoderPort;
import com.memeboo2.haemi.auth.domain.port.TokenPort;
import com.memeboo2.haemi.auth.domain.port.TotpPort;
import com.memeboo2.haemi.auth.infrastructure.security.InstitutionAdminProperties;
import com.memeboo2.haemi.auth.domain.repository.MemberRepository;
import com.memeboo2.haemi.auth.domain.repository.EmailVerificationRepository;
import com.memeboo2.haemi.auth.domain.port.VerificationEmailPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock PasswordEncoderPort passwordEncoder;
    @Mock TokenPort tokenPort;
    @Mock TotpPort totpPort;
    @Mock EmailVerificationRepository emailVerifications;
    @Mock VerificationEmailPort verificationEmail;

    private AuthApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AuthApplicationService(memberRepository, passwordEncoder, tokenPort, totpPort,
                new InstitutionAdminProperties(List.of("admin@haemi.kr")), emailVerifications, verificationEmail,
                new EmailVerificationResendRateLimiter());
    }

    @Test
    @DisplayName("회원가입 시 이메일 중복을 확인하고 비밀번호를 인코딩한다")
    void signUp_savesNormalizedMember() {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailVerifications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MemberResult result = service.signUp(
                new SignUpCommand(" USER@Example.com ", "password", "홍길동", MemberRole.FAMILY));

        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.name()).isEqualTo("홍길동");
        verify(passwordEncoder).encode("password");
        verify(memberRepository).save(argThat(member ->
                member.getEncodedPassword().equals("encoded")));
    }

    @Test
    @DisplayName("중복 이메일은 회원가입을 거부한다")
    void signUp_rejectsDuplicateEmail() {
        when(memberRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.signUp(
                new SignUpCommand("USER@example.com", "password", "홍길동", MemberRole.FAMILY)))
                .isInstanceOf(DuplicateEmailException.class);

        verifyNoInteractions(passwordEncoder);
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 성공 시 토큰을 발급하고 Refresh Token 해시를 저장한다")
    void login_issuesTokensAndStoresRefreshHash() {
        Member member = member(MemberRole.FAMILY);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);
        when(tokenPort.generateAccessToken(member.getId(), member.getEmail(), member.getRole()))
                .thenReturn("access");
        when(tokenPort.generateRefreshToken(member.getId())).thenReturn("refresh");
        when(tokenPort.hashToken("refresh")).thenReturn("refresh-hash");

        TokenResult result = service.login(
                new LoginCommand(" USER@Example.com ", "password", null));

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        assertThat(member.getRefreshTokenHash()).isEqualTo("refresh-hash");
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("정지 회원과 비밀번호 불일치는 로그인을 거부한다")
    void login_rejectsInvalidAccount() {
        Member suspended = member(MemberRole.FAMILY);
        suspended.suspend();
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.login(
                new LoginCommand("user@example.com", "password", null)))
                .isInstanceOf(AccountSuspendedException.class);

        Member active = member(MemberRole.FAMILY);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LoginCommand("user@example.com", "wrong", null)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("미확인 계정은 로그인할 수 없고, 정지 계정은 이메일 확인으로 활성화되지 않는다")
    void pendingAccountCannotLoginAndSuspendedAccountCannotVerifyEmail() {
        Member pending = Member.createUnverified("pending@example.com", "encoded", "홍길동", MemberRole.FAMILY);
        when(memberRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> service.login(new LoginCommand("pending@example.com", "password", null)))
                .isInstanceOf(EmailNotVerifiedException.class);

        Member suspended = Member.createUnverified("suspended@example.com", "encoded", "홍길동", MemberRole.FAMILY);
        suspended.suspend();
        EmailVerification verification = EmailVerification.issue(suspended.getId());
        when(emailVerifications.findByToken("token")).thenReturn(Optional.of(verification));
        when(memberRepository.findById(suspended.getId())).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.confirmEmail("token"))
                .isInstanceOf(AccountSuspendedException.class);
        assertThat(suspended.isActive()).isFalse();
    }

    @Test
    @DisplayName("TOTP 설정 회원은 코드가 없거나 틀리면 로그인을 거부한다")
    void login_validatesTotp() {
        Member member = member(MemberRole.FAMILY);
        member.enableTotp("secret");
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginCommand("user@example.com", "password", null)))
                .isInstanceOf(TotpRequiredException.class);

        when(totpPort.verifyCode("secret", "000000")).thenReturn(false);
        assertThatThrownBy(() -> service.login(
                new LoginCommand("user@example.com", "password", "000000")))
                .isInstanceOf(TotpRequiredException.class);
    }

    @Test
    @DisplayName("Refresh Token 재사용 의심 시 저장된 세션을 제거한다")
    void refresh_clearsSessionWhenStoredHashDiffers() {
        Member member = member(MemberRole.FAMILY);
        member.updateRefreshTokenHash("stored-hash");
        when(tokenPort.isValid("refresh")).thenReturn(true);
        when(tokenPort.extractMemberId("refresh")).thenReturn(member.getId());
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(tokenPort.hashToken("refresh")).thenReturn("different-hash");

        assertThatThrownBy(() -> service.refresh(new RefreshTokenCommand("refresh")))
                .isInstanceOf(InvalidRefreshTokenException.class);

        assertThat(member.getRefreshTokenHash()).isNull();
        verify(memberRepository).save(member);
    }

    @Test
    @DisplayName("비밀번호 변경 시 Access Token과 Refresh Token을 모두 무효화한다")
    void changePassword_invalidatesExistingTokens() {
        Member member = member(MemberRole.FAMILY);
        member.updateRefreshTokenHash("refresh-hash");
        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("old", "encoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("new-encoded");

        service.changePassword(
                new ChangePasswordCommand(member.getId(), "old", "new"), "access");

        assertThat(member.getEncodedPassword()).isEqualTo("new-encoded");
        assertThat(member.getRefreshTokenHash()).isNull();
        verify(tokenPort).blacklistAccessToken("access");
        verify(memberRepository).save(member);
    }

    private Member member(MemberRole role) {
        Member member = Member.create("user@example.com", "encoded", "홍길동", role);
        member.verifyEmail();
        return member;
    }
}
