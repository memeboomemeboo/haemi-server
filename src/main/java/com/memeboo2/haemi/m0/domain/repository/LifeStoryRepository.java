package com.memeboo2.haemi.m0.domain.repository;

import com.memeboo2.haemi.m0.domain.model.LifeStory;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LifeStoryRepository {
    List<LifeStory> findAllByElderId(UUID elderId);
    void replaceAll(UUID elderId, Collection<LifeStory> lifeStories);
}
