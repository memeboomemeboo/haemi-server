package com.memeboo2.haemi.m3.infrastructure.persistence;

import com.memeboo2.haemi.m3.domain.model.training.DifficultyProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaDifficultyProfileRepository extends JpaRepository<DifficultyProfile, UUID> {
    Optional<DifficultyProfile> findByElderId(String elderId);
}
