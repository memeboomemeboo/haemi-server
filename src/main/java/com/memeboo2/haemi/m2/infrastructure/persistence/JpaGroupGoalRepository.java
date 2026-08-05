package com.memeboo2.haemi.m2.infrastructure.persistence;

import com.memeboo2.haemi.m2.domain.model.goal.GoalPeriod;
import com.memeboo2.haemi.m2.domain.model.goal.GroupGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaGroupGoalRepository extends JpaRepository<GroupGoal, UUID> {

    @Query("""
            SELECT g FROM GroupGoal g
            WHERE g.albumId = :albumId
              AND g.status = 'IN_PROGRESS'
              AND g.periodStart <= :at AND g.periodEnd >= :at
            ORDER BY g.periodStart DESC
            LIMIT 1
            """)
    Optional<GroupGoal> findActive(UUID albumId, LocalDate at);

    List<GroupGoal> findByAlbumIdAndPeriodAndPeriodStart(UUID albumId, GoalPeriod period, LocalDate periodStart);
}
