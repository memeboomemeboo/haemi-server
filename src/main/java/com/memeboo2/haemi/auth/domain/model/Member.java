package com.memeboo2.haemi.auth.domain.model;

import com.memeboo2.haemi.auth.domain.event.MemberRegisteredEvent;
import com.memeboo2.haemi.auth.domain.event.MemberWithdrawnEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 회원 Aggregate Root.
 * <p>
 * 가족(FAMILY), 어르신(ELDER), 기관 관리자(INSTITUTION_ADMIN) 역할을 갖는다.
 * 기관 관리자는 2FA(TOTP)가 필수로 적용된다.
 */
@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends AbstractAggregateRoot<Member> {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    /** TOTP 비밀키 — null이면 2FA 미설정 */
    @Column(length = 64)
    private String totpSecret;

    /** 현재 유효한 Refresh Token (해시 저장) */
    @Column(length = 512)
    private String refreshTokenHash;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ───────────────── Factory ─────────────────

    public static Member create(String email, String encodedPassword, String name, MemberRole role) {
        validateEmail(email);
        validateName(name);

        Member m = new Member();
        m.id              = UUID.randomUUID();
        m.email           = email.trim().toLowerCase();
        m.encodedPassword = encodedPassword;
        m.name            = name.trim();
        m.role            = role;
        m.status          = MemberStatus.ACTIVE;
        m.createdAt       = LocalDateTime.now();
        m.updatedAt       = m.createdAt;

        m.registerEvent(new MemberRegisteredEvent(m.id, m.email, m.name, m.role));
        return m;
    }

    /** 공개 가입은 이메일 링크를 확인하기 전까지 로그인할 수 없다. */
    public static Member createUnverified(String email, String encodedPassword, String name, MemberRole role) {
        Member member = create(email, encodedPassword, name, role);
        member.status = MemberStatus.PENDING_VERIFICATION;
        return member;
    }

    // ───────────────── Commands ─────────────────

    public void changePassword(String newEncodedPassword) {
        this.encodedPassword = newEncodedPassword;
        touch();
    }

    public void updateProfile(String name) {
        validateName(name);
        this.name = name.trim();
        touch();
    }

    public void withdraw() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new AlreadyWithdrawnException(this.id);
        }
        this.status          = MemberStatus.WITHDRAWN;
        this.refreshTokenHash = null;
        touch();
        this.registerEvent(new MemberWithdrawnEvent(this.id));
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
        this.refreshTokenHash = null;
        touch();
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
        touch();
    }

    public void verifyEmail() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new AlreadyWithdrawnException(this.id);
        }
        this.emailVerifiedAt = LocalDateTime.now();
        activate();
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    // ──────── 2FA (TOTP) ────────

    public void enableTotp(String totpSecret) {
        this.totpSecret = totpSecret;
        touch();
    }

    public void disableTotp() {
        this.totpSecret = null;
        touch();
    }

    public boolean isTotpEnabled() {
        return this.totpSecret != null;
    }

    public boolean requiresTotp() {
        return this.role == MemberRole.INSTITUTION_ADMIN;
    }

    // ──────── Refresh Token ────────

    public void updateRefreshTokenHash(String hash) {
        this.refreshTokenHash = hash;
        touch();
    }

    public void clearRefreshToken() {
        this.refreshTokenHash = null;
        touch();
    }

    // ──────── Validation ────────

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidEmailException("이메일은 필수입니다.");
        }
        if (!email.contains("@")) {
            throw new InvalidEmailException("올바른 이메일 형식이 아닙니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidNameException("이름은 필수입니다.");
        }
        if (name.trim().length() > 50) {
            throw new InvalidNameException("이름은 50자를 초과할 수 없습니다.");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
