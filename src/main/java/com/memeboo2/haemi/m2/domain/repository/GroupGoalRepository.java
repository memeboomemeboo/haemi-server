package com.memeboo2.haemi.m2.domain.repository;

import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;
import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface GroupGoalRepository {

    GroupGoal save(GroupGoal goal);

    Optional<GroupGoal> findById(UUID id);

    // 지정 시점을 포함하는 진행 중 목표
    Optional<GroupGoal> findActiveByAlbumId(UUID albumId, LocalDate at);

    Optional<GroupGoal> findByAlbumIdAndPeriod(UUID albumId, GoalPeriod period, LocalDate periodStart);
}
