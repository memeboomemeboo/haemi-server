package com.memeboo2.haemi.auth.domain.repository;

import com.memeboo2.haemi.auth.domain.model.EmailVerification;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository {
    EmailVerification save(EmailVerification verification);
    Optional<EmailVerification> findByToken(String token);
    void deleteByMemberId(UUID memberId);
}
