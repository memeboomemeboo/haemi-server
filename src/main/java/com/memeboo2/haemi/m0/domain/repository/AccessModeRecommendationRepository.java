package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccessModeRecommendationRepository {

    AccessModeRecommendation save(AccessModeRecommendation recommendation);

    Optional<AccessModeRecommendation> findById(UUID id);

    // 어르신의 최신 추천(제안·적용 이력 조회용)
    Optional<AccessModeRecommendation> findLatestByElderId(UUID elderId);

    // 14일 재평가 대상: 적용 시각이 기준 이전인 어르신 (중복 제거)
    List<UUID> findElderIdsAppliedBefore(LocalDateTime cutoff);
}
