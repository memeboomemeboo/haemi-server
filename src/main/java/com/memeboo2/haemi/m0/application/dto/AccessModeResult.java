package com.memeboo2.haemi.m0.application.dto;

import com.memeboo2.haemi.m0.domain.model.ElderAccessMode;
import com.memeboo2.haemi.m0.domain.model.access.AccessModeRecommendation;
import com.memeboo2.haemi.m0.domain.model.access.EntryPath;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationSource;
import com.memeboo2.haemi.m0.domain.model.access.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccessModeResult(
        UUID elderId,
        ElderAccessMode currentMode,
        UUID recommendationId,
        ElderAccessMode recommendedMode,
        RecommendationSource source,
        RecommendationStatus status,
        EntryPath entryPath,
        UUID operatorId,
        LocalDateTime createdAt,
        LocalDateTime appliedAt
) {
    public static AccessModeResult of(ElderAccessMode currentMode, AccessModeRecommendation r) {
        return new AccessModeResult(
                r.getElderId(), currentMode, r.getId(), r.getRecommendedMode(),
                r.getSource(), r.getStatus(), r.getEntryPath(), r.getOperatorId(),
                r.getCreatedAt(), r.getAppliedAt());
    }
}
