package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;
import com.memeboo2.haemi.m0.domain.repository.AccessModeRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AccessModeRecommendationRepositoryAdapter implements AccessModeRecommendationRepository {

    private final JpaAccessModeRecommendationRepository jpa;

    @Override
    public AccessModeRecommendation save(AccessModeRecommendation recommendation) {
        return jpa.save(recommendation);
    }

    @Override
    public Optional<AccessModeRecommendation> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<AccessModeRecommendation> findLatestByElderId(UUID elderId) {
        List<AccessModeRecommendation> found = jpa.findLatest(elderId);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<UUID> findElderIdsAppliedBefore(LocalDateTime cutoff) {
        return jpa.findElderIdsAppliedBefore(cutoff);
    }
}
