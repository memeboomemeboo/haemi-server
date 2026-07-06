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

    @Column(name = "consecutive_correct", nullable = false)
    private int consecutiveCorrect;

    @Column(name = "consecutive_wrong", nullable = false)
    private int consecutiveWrong;

    @Column(name = "last_avg_response_seconds")
    private double lastAverageResponseSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "difficulty_profile_accuracy_history",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    @OrderColumn(name = "history_order")
    @Column(name = "accuracy_rate", nullable = false)
    private List<Double> recentAccuracyRates = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "difficulty_profile_wrong_patterns",
            joinColumns = @JoinColumn(name = "profile_id")
    )
    private List<WrongAnswerPattern> wrongAnswerPatterns = new ArrayList<>();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static DifficultyProfile defaultFor(String elderId) {
        DifficultyProfile profile = new DifficultyProfile();
        profile.id = UUID.randomUUID();
        profile.elderId = elderId;
        profile.currentLevel = 2;
        profile.consecutiveCorrect = 0;
        profile.consecutiveWrong = 0;
        profile.updatedAt = LocalDateTime.now();
        return profile;
    }

    public DifficultyAdjustment applySession(List<QuestionPerformance> performances,
                                             double accuracyRate,
                                             double averageResponseSeconds,
                                             DifficultyPolicy policy) {
        int previousLevel = currentLevel;
        if (performances == null || performances.isEmpty()) {
            return adjustment(previousLevel, false);
        }

        Double previousAccuracy = recentAccuracyRates.isEmpty()
                ? null
                : recentAccuracyRates.getLast();
        appendAccuracy(accuracyRate);
        boolean extremeScore = previousAccuracy != null
                && Math.abs(clampRate(accuracyRate) - previousAccuracy) >= EXTREME_SCORE_GAP;

        for (QuestionPerformance performance : performances) {
            if (performance.correct() && !performance.timeout()) {
                consecutiveCorrect++;
                consecutiveWrong = 0;
                findPattern(performance.patternKey())
                        .ifPresent(pattern -> pattern.reset(performance.questionId()));
            } else {
                consecutiveWrong++;
                consecutiveCorrect = 0;
                findPattern(performance.patternKey())
                        .ifPresentOrElse(
                                pattern -> pattern.recordFailure(performance.questionId()),
                                () -> wrongAnswerPatterns.add(WrongAnswerPattern.firstFailure(performance))
                        );
            }
        }
        this.lastAverageResponseSeconds = Math.max(averageResponseSeconds, 0.0);

        boolean timeout = performances.stream().anyMatch(QuestionPerformance::timeout);
        boolean enoughHistoryForOutlier = recentAccuracyRates.size() == MOVING_AVERAGE_WINDOW;
        double movingAverage = getThreeSessionMovingAverage();
        boolean decreaseSignal = consecutiveWrong >= 3;
        boolean increaseSignal = consecutiveCorrect >= 3
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
        Set<QuestionType> recommended = new LinkedHashSet<>();
        wrongAnswerPatterns.stream()
                .filter(WrongAnswerPattern::isRepeated)
                .map(WrongAnswerPattern::getQuestionType)
                .filter(policy.getQuestionTypes()::contains)
                .forEach(recommended::add);
        recommended.addAll(policy.getQuestionTypes());
        return List.copyOf(recommended);
    }

    public double getThreeSessionMovingAverage() {
        return recentAccuracyRates.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public List<Double> getRecentAccuracyRates() {
        return Collections.unmodifiableList(recentAccuracyRates);
    }

    public List<WrongAnswerPattern> getWrongAnswerPatterns() {
        return Collections.unmodifiableList(wrongAnswerPatterns);
    }

    private void appendAccuracy(double accuracyRate) {
        recentAccuracyRates.add(clampRate(accuracyRate));
        if (recentAccuracyRates.size() > MOVING_AVERAGE_WINDOW) {
            recentAccuracyRates.removeFirst();
        }
    }

    private Optional<WrongAnswerPattern> findPattern(String patternKey) {
        return wrongAnswerPatterns.stream()
                .filter(pattern -> pattern.getPatternKey().equals(patternKey))
                .findFirst();
    }

    private DifficultyAdjustment adjustment(int previousLevel, boolean extremeScore) {
        List<String> repeatedWrongQuestionIds = wrongAnswerPatterns.stream()
                .filter(WrongAnswerPattern::isRepeated)
                .map(WrongAnswerPattern::getLastQuestionId)
                .toList();
        return new DifficultyAdjustment(
                previousLevel,
                currentLevel,
                getThreeSessionMovingAverage(),
                extremeScore,
                repeatedWrongQuestionIds
        );
    }

    private void resetStreaks() {
        consecutiveWrong = 0;
        consecutiveCorrect = 0;
    }

    private static double clampRate(double rate) {
        return Math.max(0.0, Math.min(1.0, rate));
    }
}
