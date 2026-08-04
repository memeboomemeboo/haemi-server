package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.ElderHealth;
import com.memeboo2.haemi.m0.domain.repository.ElderHealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ElderHealthRepositoryAdapter implements ElderHealthRepository {

    private final JpaElderHealthRepository health;

    @Override
    public ElderHealth save(ElderHealth elderHealth) {
        return health.save(elderHealth);
    }

    @Override
    public Optional<ElderHealth> findByElderId(UUID elderId) {
        return health.findById(elderId);
    }

    @Override
    public void deleteByElderId(UUID elderId) {
        health.deleteById(elderId);
    }
}
