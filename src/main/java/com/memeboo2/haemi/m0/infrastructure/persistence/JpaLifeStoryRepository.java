package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.LifeStory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaLifeStoryRepository extends JpaRepository<LifeStory, UUID> {
    List<LifeStory> findAllByElderId(UUID elderId);
    void deleteAllByElderId(UUID elderId);
}
