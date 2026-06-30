package com.memeboo2.haemi.m4.domain.repository;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveChangeAlert;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CognitiveChangeAlertRepository {
    CognitiveChangeAlert save(CognitiveChangeAlert alert);
    Optional<CognitiveChangeAlert> findLatestByElderIdSince(String elderId, LocalDateTime since);
}
