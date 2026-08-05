package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaAccessModeRecommendationRepository
        extends JpaRepository<AccessModeRecommendation, UUID> {

    @Query("""
            SELECT r FROM AccessModeRecommendation r
            WHERE r.elderId = :elderId
            ORDER BY r.createdAt DESC
            LIMIT 1
            """)
    List<AccessModeRecommendation> findLatest(UUID elderId);

    @Query("""
            SELECT DISTINCT r.elderId FROM AccessModeRecommendation r
            WHERE r.status = com.memeboo2.haemi.m0.domain.model.access.RecommendationStatus.APPLIED
              AND r.appliedAt < :cutoff
            """)
    List<UUID> findElderIdsAppliedBefore(LocalDateTime cutoff);
}
