package com.memeboo2.haemi.m4.domain.repository;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveChangeAlert;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CognitiveChangeAlertRepository {
    CognitiveChangeAlert save(CognitiveChangeAlert alert);
    Optional<CognitiveChangeAlert> findById(UUID alertId);
    Optional<CognitiveChangeAlert> findLatestByElderIdSince(String elderId, LocalDateTime since);
    boolean existsFalsePositiveByElderIdSince(String elderId, LocalDateTime since);
}
