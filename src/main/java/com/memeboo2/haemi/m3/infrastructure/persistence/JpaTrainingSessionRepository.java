package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.CognitiveTrainingSession;
import com.memeboo2.haemi.m3.domain.model.training.TrainingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaTrainingSessionRepository extends JpaRepository<CognitiveTrainingSession, UUID> {
    Optional<CognitiveTrainingSession> findByElderIdAndSessionDate(String elderId, LocalDate sessionDate);
    List<CognitiveTrainingSession> findByAlbumIdAndSessionDateBetweenAndStatus(
            UUID albumId, LocalDate from, LocalDate to, TrainingSessionStatus status);
}
