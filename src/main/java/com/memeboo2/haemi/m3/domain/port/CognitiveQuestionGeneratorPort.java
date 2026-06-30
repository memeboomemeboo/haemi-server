package com.memeboo2.haemi.m3.domain.port;

import com.memeboo2.haemi.m3.domain.model.training.TrainingQuestion;

import java.util.List;
import java.util.UUID;

public interface CognitiveQuestionGeneratorPort {
    List<TrainingQuestion> generate(String elderId, UUID albumId, int difficultyLevel);
}
