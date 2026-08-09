package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.ElderHealth;

import java.util.Optional;
import java.util.UUID;

public interface ElderHealthRepository {
    ElderHealth save(ElderHealth elderHealth);
    Optional<ElderHealth> findByElderId(UUID elderId);
    void deleteByElderId(UUID elderId);
}
