package com.memeboo2.haemi.auth.infrastructure.persistence;

import com.memeboo2.haemi.auth.domain.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface JpaEmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByToken(String token);
    void deleteByMemberId(UUID memberId);
}
