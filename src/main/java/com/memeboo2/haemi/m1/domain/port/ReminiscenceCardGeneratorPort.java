package com.memeboo2.haemi.m1.domain.port;

import com.memeboo2.haemi.m1.domain.model.reminiscence.GeneratedCard;

import java.util.Optional;

public interface ReminiscenceCardGeneratorPort {
    Optional<GeneratedCard> generate(CardGenerationRequest request);

    record CardGenerationRequest(java.util.UUID photoId, String factualContext, String personContext,
                                 int personalizationLevel, java.util.List<String> sensitiveTopics) {
    }
}
