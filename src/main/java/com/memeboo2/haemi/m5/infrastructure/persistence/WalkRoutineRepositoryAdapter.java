package com.memeboo2.haemi.m5.infrastructure.persistence;

import com.memeboo2.haemi.m5.domain.model.care.WalkRoutine;
import com.memeboo2.haemi.m5.domain.repository.WalkRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WalkRoutineRepositoryAdapter implements WalkRoutineRepository {

    private final JpaWalkRoutineRepository jpa;

    @Override
    public WalkRoutine save(WalkRoutine routine) {
        return jpa.save(routine);
    }

    @Override
    public Optional<WalkRoutine> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<WalkRoutine> findActiveByElderId(String elderId) {
        return jpa.findByElderIdAndActiveTrue(elderId);
    }

    @Override
    public List<WalkRoutine> findAllActive() {
        return jpa.findByActiveTrue();
    }
}
