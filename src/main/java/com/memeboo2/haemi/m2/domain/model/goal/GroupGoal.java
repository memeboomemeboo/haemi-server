package com.memeboo2.haemi.m2.domain.model.goal;

import com.memeboo2.haemi.m2.domain.event.GroupGoalAchievedEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 그룹 협력 목표 (F1-03-A). #42에서 폐기한 개인 랭킹의 대체 개념.
 * 개인 순위·뱃지·스트릭 없이 가족 전체가 하나의 목표를 함께 채운다.
 */
@Entity
@Table(name = "group_goals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"album_id", "period", "period_start"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupGoal extends AbstractAggregateRoot<GroupGoal> {

    private static final int MIN_TARGET = 1;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "album_id", nullable = false)
    private UUID albumId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private GoalPeriod period;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "target_count", nullable = false)
    private int targetCount;

    @Column(name = "current_progress", nullable = false)
    private int currentProgress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GoalStatus status;

    // 순위가 아니라 "누가 함께했는지"만 담는 참여자 집합 (협력감)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "group_goal_participants",
            joinColumns = @JoinColumn(name = "goal_id"))
    @Column(name = "member_id")
    private Set<String> participantIds = new LinkedHashSet<>();

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ── 팩토리 ──
    public static GroupGoal start(UUID albumId, GoalPeriod period, LocalDate onDate, int targetCount) {
        if (targetCount < MIN_TARGET) {
            throw new InvalidGoalTargetException(targetCount);
        }
        GroupGoal goal = new GroupGoal();
        goal.id              = UUID.randomUUID();
        goal.albumId         = albumId;
        goal.period          = period;
        goal.periodStart     = period.startOf(onDate);
        goal.periodEnd       = period.endOf(onDate);
        goal.targetCount     = targetCount;
        goal.currentProgress = 0;
        goal.status          = GoalStatus.IN_PROGRESS;
        goal.createdAt       = LocalDateTime.now();
        return goal;
    }

    // ── 협력 진척 누적 ──
    public void recordProgress(int amount, String contributorId) {
        if (amount <= 0) return;
        if (contributorId != null && !contributorId.isBlank()) {
            participantIds.add(contributorId);
        }
        if (status == GoalStatus.ACHIEVED) return; // 달성 후에도 참여자는 계속 기록, 진척/이벤트는 고정

        this.currentProgress += amount;
        if (currentProgress >= targetCount) {
            this.currentProgress = targetCount;
            this.status          = GoalStatus.ACHIEVED;
            this.achievedAt      = LocalDateTime.now();
            registerEvent(new GroupGoalAchievedEvent(
                    id, albumId, period, targetCount, participantIds.size(), achievedAt));
        }
    }

    public boolean isAchieved() {
        return status == GoalStatus.ACHIEVED;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(periodStart) && !date.isAfter(periodEnd);
    }

    public int remaining() {
        return Math.max(0, targetCount - currentProgress);
    }

    public int participantCount() {
        return participantIds.size();
    }

    public Set<String> getParticipantIds() {
        return Collections.unmodifiableSet(participantIds);
    }
}
