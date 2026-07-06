package com.memeboo2.haemi.m3.domain.repository;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyPolicy;

import java.util.List;
import java.util.Optional;

public interface DifficultyPolicyRepository {
    DifficultyPolicy save(DifficultyPolicy policy);
    Optional<DifficultyPolicy> findByLevel(int level);
    List<DifficultyPolicy> findAll();
}
