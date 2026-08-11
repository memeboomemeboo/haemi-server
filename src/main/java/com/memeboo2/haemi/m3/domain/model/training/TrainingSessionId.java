package com.memeboo2.haemi.m3.domain.model.training;

import com.memeboo2.haemi.common.support.DomainIds;

import java.util.UUID;

public record TrainingSessionId(UUID value) {
    public static TrainingSessionId of(String value) {
        return new TrainingSessionId(DomainIds.parseUuid(value, "훈련 세션 ID"));
    }

    public static TrainingSessionId of(UUID value) {
        return new TrainingSessionId(value);
    }
}
