package com.memeboo2.haemi.auth.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "member_id", nullable = false, columnDefinition = "uuid")
    private UUID memberId;

    @Column(nullable = false, unique = true, length = 96)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static EmailVerification issue(UUID memberId) {
        EmailVerification verification = new EmailVerification();
        verification.id = UUID.randomUUID();
        verification.memberId = memberId;
        verification.token = newToken();
        verification.createdAt = LocalDateTime.now();
        verification.expiresAt = verification.createdAt.plusHours(24);
        return verification;
    }

    public void consume() {
        if (usedAt != null || LocalDateTime.now().isAfter(expiresAt)) {
            throw new EmailVerificationInvalidException();
        }
        usedAt = LocalDateTime.now();
    }

    private static String newToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
