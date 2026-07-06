package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;
import com.memeboo2.haemi.m3.domain.repository.DifficultyPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DifficultyPolicyRepositoryAdapter implements DifficultyPolicyRepository {

    private final JpaDifficultyPolicyRepository jpa;

    @Override
    public DifficultyPolicy save(DifficultyPolicy policy) {
        return jpa.save(policy);
    }

    @Override
    public Optional<DifficultyPolicy> findByLevel(int level) {
        return jpa.findById(level);
    }

    @Override
    public List<DifficultyPolicy> findAll() {
        return jpa.findAll().stream()
                .sorted(java.util.Comparator.comparingInt(DifficultyPolicy::getLevel))
                .toList();
    }
}
