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
    private static final double EXTREME_SCORE_GAP = 0.75;

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "elder_id", nullable = false)
    private String elderId;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "consecutive_responded", nullable = false)
    private int consecutiveResponded;

    @Column(name = "consecutive_no_response", nullable = false)
    private int consecutiveNoResponse;

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
        profile.consecutiveResponded = 0;
        profile.consecutiveNoResponse = 0;
        profile.updatedAt = LocalDateTime.now();
        return profile;
    }

    public DifficultyAdjustment applySession(List<QuestionPerformance> performances,
                                             double responseRate,
                                             double averageResponseSeconds,
                                             DifficultyPolicy policy) {
        int previousLevel = currentLevel;
        if (performances == null || performances.isEmpty()) {
            return adjustment(previousLevel, false);
        }

        Double previousResponseRate = recentResponseRates.isEmpty()
                ? null
                : recentResponseRates.getLast();
        appendResponseRate(responseRate);
        boolean extremeScore = previousResponseRate != null
                && Math.abs(clampRate(responseRate) - previousResponseRate) >= EXTREME_SCORE_GAP;

        for (QuestionPerformance performance : performances) {
            if (performance.responded() && !performance.timeout()) {
                consecutiveResponded++;
                consecutiveNoResponse = 0;
            } else {
                consecutiveNoResponse++;
                consecutiveResponded = 0;
            }
        }
        this.lastAverageResponseSeconds = Math.max(averageResponseSeconds, 0.0);

        boolean timeout = performances.stream().anyMatch(QuestionPerformance::timeout);
        boolean enoughHistoryForOutlier = recentResponseRates.size() == MOVING_AVERAGE_WINDOW;
        double movingAverage = getThreeSessionMovingAverage();
        boolean decreaseSignal = consecutiveNoResponse >= 3;
        boolean increaseSignal = consecutiveResponded >= 3
                && lastAverageResponseSeconds <= policy.getMaxAverageResponseSeconds();

        if (extremeScore && !timeout) {
            if (!enoughHistoryForOutlier) {
                decreaseSignal = false;
                increaseSignal = false;
            } else {
                decreaseSignal = decreaseSignal
                        && movingAverage <= policy.getDecreaseAccuracyThreshold();
                increaseSignal = increaseSignal
                        && movingAverage >= policy.getIncreaseAccuracyThreshold();
            }
        }

        if (timeout || decreaseSignal) {
            currentLevel = Math.max(1, currentLevel - 1);
            resetStreaks();
        } else if (increaseSignal) {
            currentLevel = Math.min(5, currentLevel + 1);
            resetStreaks();
        }
        this.updatedAt = LocalDateTime.now();
        return adjustment(previousLevel, extremeScore);
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

    private DifficultyAdjustment adjustment(int previousLevel, boolean extremeScore) {
        return new DifficultyAdjustment(
                previousLevel,
                currentLevel,
                getThreeSessionMovingAverage(),
                extremeScore,
                List.of()
        );
    }

    private void resetStreaks() {
        consecutiveNoResponse = 0;
        consecutiveResponded = 0;
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
