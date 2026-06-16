package com.memeboo2.haemi.m3.domain.repository;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyProfile;

import java.util.Optional;

public interface DifficultyProfileRepository {
    DifficultyProfile save(DifficultyProfile profile);
    Optional<DifficultyProfile> findByElderId(String elderId);
}
