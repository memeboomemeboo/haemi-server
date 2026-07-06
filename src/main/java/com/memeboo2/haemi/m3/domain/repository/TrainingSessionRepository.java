package com.memeboo2.haemi.m3.domain.repository;

import com.memeboo2.haemi.m3.domain.model.training.CognitiveTrainingSession;
import com.memeboo2.haemi.m3.domain.model.training.TrainingSessionId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingSessionRepository {
    CognitiveTrainingSession save(CognitiveTrainingSession session);
    Optional<CognitiveTrainingSession> findById(TrainingSessionId id);
    Optional<CognitiveTrainingSession> findByElderIdAndSessionDate(String elderId, LocalDate sessionDate);
    Optional<CognitiveTrainingSession> findLatestCompleted(String elderId, UUID albumId);
    List<CognitiveTrainingSession> findCompletedByAlbumIdAndDateBetween(UUID albumId, LocalDate from, LocalDate to);
}
