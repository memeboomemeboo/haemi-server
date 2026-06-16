package com.memeboo2.haemi.m3.domain.model.training;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "difficulty_profiles",
        uniqueConstraints = @UniqueConstraint(columnNames = "elder_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DifficultyProfile {

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

    public void applyAttempts(List<QuestionAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) return;
        for (QuestionAttempt attempt : attempts) {
            if (attempt.isCorrect() && !attempt.isTimeout()) {
                consecutiveCorrect++;
                consecutiveWrong = 0;
            } else {
                consecutiveWrong++;
                consecutiveCorrect = 0;
            }
        }
        this.lastAverageResponseSeconds = attempts.stream()
                .mapToInt(QuestionAttempt::getResponseSeconds)
                .average()
                .orElse(0.0);

        if (consecutiveWrong >= 3 || attempts.stream().anyMatch(QuestionAttempt::isTimeout)) {
            currentLevel = Math.max(1, currentLevel - 1);
            consecutiveWrong = 0;
            consecutiveCorrect = 0;
        } else if (consecutiveCorrect >= 3 && lastAverageResponseSeconds <= thresholdSeconds(currentLevel)) {
            currentLevel = Math.min(5, currentLevel + 1);
            consecutiveCorrect = 0;
            consecutiveWrong = 0;
        }
        this.updatedAt = LocalDateTime.now();
    }

    private int thresholdSeconds(int level) {
        return switch (level) {
            case 1 -> 20;
            case 2 -> 25;
            case 3 -> 30;
            case 4 -> 40;
            default -> 50;
        };
    }
}
