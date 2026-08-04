package com.memeboo2.haemi.m0.infrastructure.persistence;

import com.memeboo2.haemi.m0.domain.model.LifeStory;
import com.memeboo2.haemi.m0.domain.repository.LifeStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LifeStoryRepositoryAdapter implements LifeStoryRepository {

    private final JpaLifeStoryRepository lifeStories;

    @Override
    public List<LifeStory> findAllByElderId(UUID elderId) {
        return lifeStories.findAllByElderId(elderId);
    }

    @Override
    public void replaceAll(UUID elderId, Collection<LifeStory> newLifeStories) {
        lifeStories.deleteAllByElderId(elderId);
        lifeStories.saveAll(newLifeStories);
    }
}
