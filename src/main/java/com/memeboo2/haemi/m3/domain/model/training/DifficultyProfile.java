package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "difficulty_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "elder_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DifficultyProfile {

    private static final int MOVING_AVERAGE_WINDOW = 3;
    // F3-02: 상향은 2주기 연속 충족 시에만, 하향은 즉시
    private static final int INCREASE_CYCLE_REQUIREMENT = 2;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    // 상향 기준을 연속 충족한 세션 수 (2주기 도달 시 상향)
    @Column(name = "increase_eligible_sessions", nullable = false)
    private int increaseEligibleSessions;

    @Column(name = "last_avg_response_seconds")
    private double lastAverageResponseSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "difficulty_profile_response_history",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    @OrderColumn(name = "history_order")
    @Column(name = "response_rate", nullable = false)
    private List<Double> recentResponseRates = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DifficultyProfile defaultFor(String elderId) {
        DifficultyProfile profile = new DifficultyProfile();
        profile.id = UUID.randomUUID();
        profile.elderId = elderId;
        profile.currentLevel = 2;
        profile.increaseEligibleSessions = 0;
        profile.updatedAt = LocalDateTime.now();
        return profile;
    }

    /**
     * F3-02 발화 기반 개인화: 세션 발화율(responseRate)을 1순위 신호로 난이도를 조정한다.
     * - 하향 즉시: 발화율이 하향 기준 이하이거나 시간 초과가 있으면 그 세션에서 바로 한 단계 낮춘다.
     * - 상향 2주기: 발화율이 상향 기준 이상이고 평균 반응 시간이 기준 이내인 세션이
     *   2회 연속될 때만 한 단계 높인다.
     */
    public DifficultyAdjustment applySession(List<QuestionPerformance> performances,
                                             double responseRate,
                                             double averageResponseSeconds,
                                             DifficultyPolicy policy) {
        int previousLevel = currentLevel;
        if (performances == null || performances.isEmpty()) {
            return adjustment(previousLevel, false);
        }

        double rate = clampRate(responseRate);
        appendResponseRate(rate);
        this.lastAverageResponseSeconds = Math.max(averageResponseSeconds, 0.0);
        boolean timeout = performances.stream().anyMatch(QuestionPerformance::timeout);

        boolean increaseCriteriaMet = rate >= policy.getIncreaseAccuracyThreshold()
                && lastAverageResponseSeconds <= policy.getMaxAverageResponseSeconds();
        boolean increaseBuffered = false;

        if (timeout || rate <= policy.getDecreaseAccuracyThreshold()) {
            currentLevel = Math.max(1, currentLevel - 1);
            increaseEligibleSessions = 0;
        } else if (increaseCriteriaMet) {
            increaseEligibleSessions++;
            if (increaseEligibleSessions >= INCREASE_CYCLE_REQUIREMENT) {
                currentLevel = Math.min(5, currentLevel + 1);
                increaseEligibleSessions = 0;
            } else {
                increaseBuffered = true;
            }
        } else {
            increaseEligibleSessions = 0;
        }

        this.updatedAt = LocalDateTime.now();
        return adjustment(previousLevel, increaseBuffered);
    }

    public List<QuestionType> recommendQuestionTypes(DifficultyPolicy policy) {
        return List.copyOf(policy.getQuestionTypes());
    }

    public double getThreeSessionMovingAverage() {
        return recentResponseRates.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public List<Double> getRecentResponseRates() {
        return Collections.unmodifiableList(recentResponseRates);
    }

    private void appendResponseRate(double responseRate) {
        recentResponseRates.add(clampRate(responseRate));
        if (recentResponseRates.size() > MOVING_AVERAGE_WINDOW) {
            recentResponseRates.removeFirst();
        }
    }

    private DifficultyAdjustment adjustment(int previousLevel, boolean increaseBuffered) {
        return new DifficultyAdjustment(
                previousLevel,
                currentLevel,
                getThreeSessionMovingAverage(),
                increaseBuffered,
                List.of()
        );
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
