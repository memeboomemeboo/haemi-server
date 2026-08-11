package com.memeboo2.haemi.auth.infrastructure.persistence;

import com.memeboo2.haemi.auth.domain.model.EmailVerification;
import com.memeboo2.haemi.auth.domain.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmailVerificationRepositoryAdapter implements EmailVerificationRepository {
    private final JpaEmailVerificationRepository jpa;
    public EmailVerification save(EmailVerification verification) { return jpa.save(verification); }
    public Optional<EmailVerification> findByToken(String token) { return jpa.findByToken(token); }
    public void deleteByMemberId(UUID memberId) { jpa.deleteByMemberId(memberId); }
}
