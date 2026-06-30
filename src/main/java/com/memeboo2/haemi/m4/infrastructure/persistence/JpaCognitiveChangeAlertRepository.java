package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveChangeAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface JpaCognitiveChangeAlertRepository extends JpaRepository<CognitiveChangeAlert, UUID> {
    Optional<CognitiveChangeAlert> findFirstByElderIdAndSentAtAfterOrderBySentAtDesc(String elderId, LocalDateTime since);
}
