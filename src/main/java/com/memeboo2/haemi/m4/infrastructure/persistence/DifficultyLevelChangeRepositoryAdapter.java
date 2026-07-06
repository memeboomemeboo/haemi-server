package com.memeboo2.haemi.m4.infrastructure.persistence;

import com.memeboo2.haemi.m4.domain.model.dashboard.DifficultyLevelChange;
import com.memeboo2.haemi.m4.domain.repository.DifficultyLevelChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DifficultyLevelChangeRepositoryAdapter implements DifficultyLevelChangeRepository {

    private final JpaDifficultyLevelChangeRepository jpa;

    @Override
    public DifficultyLevelChange save(DifficultyLevelChange change) {
        return jpa.save(change);
    }
}
