package com.memeboo2.haemi.m2.domain.model.goal;

import com.memeboo2.haemi.m2.domain.event.GroupGoalAchievedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupGoalTest {

    private final UUID albumId = UUID.randomUUID();
    private final LocalDate onDate = LocalDate.of(2026, 8, 5); // 수요일

    @Test
    @DisplayName("주간 목표는 해당 주(월~일) 기간으로 개시되고 진행 중 상태다")
    void start_setsWeeklyPeriod() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 20);

        assertThat(goal.getStatus()).isEqualTo(GoalStatus.IN_PROGRESS);
        assertThat(goal.getPeriodStart()).isEqualTo(LocalDate.of(2026, 8, 3)); // 월
        assertThat(goal.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 8, 9));   // 일
        assertThat(goal.remaining()).isEqualTo(20);
    }

    @Test
    @DisplayName("목표 값이 1 미만이면 개시할 수 없다")
    void start_rejectsNonPositiveTarget() {
        assertThatThrownBy(() -> GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 0))
                .isInstanceOf(InvalidGoalTargetException.class);
    }

    @Test
    @DisplayName("진척이 누적되고 참여자는 중복 없이 집계된다")
    void recordProgress_accumulatesAndTracksDistinctParticipants() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 20);

        goal.recordProgress(1, "member-1");
        goal.recordProgress(1, "member-2");
        goal.recordProgress(1, "member-1");

        assertThat(goal.getCurrentProgress()).isEqualTo(3);
        assertThat(goal.participantCount()).isEqualTo(2);
        assertThat(goal.getParticipantIds()).containsExactly("member-1", "member-2");
    }

    @Test
    @DisplayName("0 이하 진척은 무시된다")
    void recordProgress_ignoresNonPositiveAmount() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 20);

        goal.recordProgress(0, "member-1");
        goal.recordProgress(-5, "member-1");

        assertThat(goal.getCurrentProgress()).isZero();
        assertThat(goal.participantCount()).isZero();
    }

    @Test
    @DisplayName("목표 도달 시 진척은 목표치로 고정되고 달성 이벤트가 1회 발행된다")
    void recordProgress_achievesAndPublishesEventOnce() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 3);

        goal.recordProgress(2, "member-1");
        goal.recordProgress(5, "member-2"); // 초과분은 목표치로 캡

        assertThat(goal.isAchieved()).isTrue();
        assertThat(goal.getCurrentProgress()).isEqualTo(3);
        assertThat(goal.remaining()).isZero();
        assertThat(goal.getAchievedAt()).isNotNull();
        assertThat(achievedEvents(goal)).hasSize(1);

        // 달성 이후 추가 진척은 진척/이벤트를 바꾸지 않지만 참여자는 계속 기록
        goal.recordProgress(1, "member-3");
        assertThat(goal.getCurrentProgress()).isEqualTo(3);
        assertThat(achievedEvents(goal)).hasSize(1);
        assertThat(goal.participantCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("covers는 기간 포함 여부를 경계 포함으로 판정한다")
    void covers_isInclusive() {
        GroupGoal goal = GroupGoal.start(albumId, GoalPeriod.WEEKLY, onDate, 20);

        assertThat(goal.covers(LocalDate.of(2026, 8, 3))).isTrue();
        assertThat(goal.covers(LocalDate.of(2026, 8, 9))).isTrue();
        assertThat(goal.covers(LocalDate.of(2026, 8, 10))).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> achievedEvents(GroupGoal goal) {
        Collection<Object> events =
                (Collection<Object>) ReflectionTestUtils.invokeMethod(goal, "domainEvents");
        return events.stream()
                .filter(e -> e instanceof GroupGoalAchievedEvent)
                .map(e -> (Object) e)
                .toList();
    }
}
