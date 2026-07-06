package com.memeboo2.haemi.m4.domain.repository;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;

import java.util.Optional;
import java.util.UUID;

public interface CognitiveReportRepository {
    CognitiveReport save(CognitiveReport report);

    Optional<CognitiveReport> findById(UUID id);
}
