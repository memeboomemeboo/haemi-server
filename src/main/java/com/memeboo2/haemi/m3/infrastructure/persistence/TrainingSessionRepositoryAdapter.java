package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.CognitiveTrainingSession;
import com.memeboo2.haemi.m3.domain.model.training.TrainingSessionId;
import com.memeboo2.haemi.m3.domain.model.training.TrainingSessionStatus;
import com.memeboo2.haemi.m3.domain.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TrainingSessionRepositoryAdapter implements TrainingSessionRepository {

    private final JpaTrainingSessionRepository jpa;

    @Override
    public CognitiveTrainingSession save(CognitiveTrainingSession session) {
        return jpa.save(session);
    }

    @Override
    public Optional<CognitiveTrainingSession> findById(TrainingSessionId id) {
        return jpa.findById(id.value());
    }

    @Override
    public Optional<CognitiveTrainingSession> findByElderIdAndSessionDate(String elderId, LocalDate sessionDate) {
        return jpa.findByElderIdAndSessionDate(elderId, sessionDate);
    }

    @Override
    public List<CognitiveTrainingSession> findCompletedByAlbumIdAndDateBetween(UUID albumId, LocalDate from, LocalDate to) {
        return jpa.findByAlbumIdAndSessionDateBetweenAndStatus(albumId, from, to, TrainingSessionStatus.COMPLETED);
    }
}
