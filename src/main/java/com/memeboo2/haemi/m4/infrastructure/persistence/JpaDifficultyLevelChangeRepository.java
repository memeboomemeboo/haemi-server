package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.DifficultyLevelChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDifficultyLevelChangeRepository
        extends JpaRepository<DifficultyLevelChange, UUID> {
}
