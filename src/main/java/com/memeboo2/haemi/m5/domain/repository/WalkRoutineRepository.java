package com.memeboo2.haemi.m5.domain.repository;

import com.memeboo2.haemi.m5.domain.model.care.WalkRoutine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkRoutineRepository {
    WalkRoutine save(WalkRoutine routine);
    Optional<WalkRoutine> findById(UUID id);
    List<WalkRoutine> findActiveByElderId(String elderId);
}
