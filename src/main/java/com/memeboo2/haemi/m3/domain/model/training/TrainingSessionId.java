package com.memeboo2.haemi.m3.domain.model.training;

import java.util.UUID;

public record TrainingSessionId(UUID value) {
    public static TrainingSessionId of(String value) {
        return new TrainingSessionId(UUID.fromString(value));
    }

    public static TrainingSessionId of(UUID value) {
        return new TrainingSessionId(value);
    }
}
