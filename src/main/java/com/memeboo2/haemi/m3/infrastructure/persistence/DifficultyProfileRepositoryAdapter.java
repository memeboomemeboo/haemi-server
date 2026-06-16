package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyProfile;
import com.memeboo2.haemi.m3.domain.repository.DifficultyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DifficultyProfileRepositoryAdapter implements DifficultyProfileRepository {

    private final JpaDifficultyProfileRepository jpa;

    @Override
    public DifficultyProfile save(DifficultyProfile profile) {
        return jpa.save(profile);
    }

    @Override
    public Optional<DifficultyProfile> findByElderId(String elderId) {
        return jpa.findByElderId(elderId);
    }
}
