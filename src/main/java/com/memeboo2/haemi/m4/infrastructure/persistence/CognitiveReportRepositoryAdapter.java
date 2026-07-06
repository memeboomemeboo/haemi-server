package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveReport;
import com.memeboo2.haemi.m4.domain.repository.CognitiveReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CognitiveReportRepositoryAdapter implements CognitiveReportRepository {

    private final JpaCognitiveReportRepository jpa;

    @Override
    public CognitiveReport save(CognitiveReport report) {
        return jpa.save(report);
    }

    @Override
    public Optional<CognitiveReport> findById(UUID id) {
        return jpa.findById(id);
    }
}
