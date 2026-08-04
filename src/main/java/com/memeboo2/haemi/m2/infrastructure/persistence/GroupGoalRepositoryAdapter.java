package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;
import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;
import com.memeboo2.haemi.m2.domain.repository.GroupGoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GroupGoalRepositoryAdapter implements GroupGoalRepository {

    private final JpaGroupGoalRepository jpa;

    @Override
    public GroupGoal save(GroupGoal goal) {
        return jpa.save(goal);
    }

    @Override
    public Optional<GroupGoal> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<GroupGoal> findActiveByAlbumId(UUID albumId, LocalDate at) {
        return jpa.findActive(albumId, at);
    }

    @Override
    public Optional<GroupGoal> findByAlbumIdAndPeriod(UUID albumId, GoalPeriod period, LocalDate periodStart) {
        List<GroupGoal> found = jpa.findByAlbumIdAndPeriodAndPeriodStart(albumId, period, periodStart);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }
}
