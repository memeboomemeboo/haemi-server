package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.CognitiveChangeAlert;
import com.memeboo2.haemi.m4.domain.repository.CognitiveChangeAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CognitiveChangeAlertRepositoryAdapter implements CognitiveChangeAlertRepository {

    private final JpaCognitiveChangeAlertRepository jpa;

    @Override
    public CognitiveChangeAlert save(CognitiveChangeAlert alert) {
        return jpa.save(alert);
    }

    @Override
    public Optional<CognitiveChangeAlert> findById(UUID alertId) {
        return jpa.findById(alertId);
    }

    @Override
    public Optional<CognitiveChangeAlert> findLatestByElderIdSince(String elderId, LocalDateTime since) {
        return jpa.findFirstByElderIdAndSentAtAfterOrderBySentAtDesc(elderId, since);
    }

    @Override
    public boolean existsFalsePositiveByElderIdSince(String elderId, LocalDateTime since) {
        return jpa.existsByElderIdAndFalsePositiveAtAfter(elderId, since);
    }
}
